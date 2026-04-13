package noergler

import leo.datastructures.TPTP
import noergler.ProofCheckController.{Configuration, VerificationFailedException, VerificationTimedOutException}
import noergler.checks.{ConjectureNegationCheck, CorrectFormulaFromFileCheck, FormulaNamesUniquenessCheck, GenericInferenceCheck, InferenceParentsAcyclicityCheck, InferenceParentsExistCheck, ProofEndsInFalseCheck, SkolemizationCheck}

import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.logging.Logger
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * The Controller coordinates the checking process, i.e., how and when
 * which checks are started etc (in parallel oder sequential).
 *
 * @param problem The source problem
 * @param proof The proof to be checked
 * @param configuration The checking configuration
 */
class ProofCheckController(problem: Option[TPTP.Problem],
                           proof: TPTP.Problem,
                           configuration: Configuration) {
  final val logger: Logger = Logger.getLogger("Nörgler.Controller")
  final private def threadCount: Int = Runtime.getRuntime.availableProcessors()*2
  final implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(
    Executors.newFixedThreadPool(threadCount))

  /** Map of problem file TPTP annotated formula name (hopefully unique) -> the formula */
  private var problemFormulas: Map[String, TPTP.FOFAnnotated] = Map.empty
  /** Name of the conjecture from the problem file, if known  */
  private var problemConjectureName: Option[String] = None

  /** The sequence of proof steps as they appear in the proof. */
  private var proofSteps: Seq[TPTP.FOFAnnotated] = Vector.empty
  /** Map of proof file TPTP annotated formula name (hopefully unique) -> the formula */
  private var proofFormulas: Map[String, TPTP.FOFAnnotated] = Map.empty

  private var usedSkolemSymbols: Set[String] = Set.empty
  private lazy val problemSymbols: Set[String] =
    if (problem.isDefined) problem.get.formulas.flatMap(_.symbols).toSet
    else {
      // No problem file given -> reconstruct based on the problem formulas given in proof
      // todo: instead use the forumulas with annotation "file" here
      val problemFormulaRoles = Seq("axiom", "hypothesis", "definition", "assumption", "lemma", "theorem", "corollary", "conjecture", "negated_conjecture")
      val problemFormulas = proof.formulas.filter(f => problemFormulaRoles.contains(f.role))
      problemFormulas.flatMap(_.symbols).toSet
    }

  private var openFutures: Seq[Future[Any]] = Seq.empty

  final def apply(): Result = {
    //////////////////////////////////////////////////////////
    // preliminary steps
    //////////////////////////////////////////////////////////
    logger.config(s"Used configuration: ${configuration.toString}")
    if (problem.isDefined){
      logger.fine("Processing problem file ...")
      // process problem file, read to map, initialize conjecture name
      for (af <- problem.get.formulas) {
        af match {
          case f@TPTP.FOFAnnotated(name, "conjecture", _, _) =>
            problemFormulas += (name -> f)
            problemConjectureName = Some(name)
          case f@TPTP.FOFAnnotated(name, _, _, _) =>
            problemFormulas += (name -> f)
          case _ => throw new IllegalArgumentException("Only FOF input allowed at the moment.")
        }
      }
      logger.info(s"Conjecture in problem found: ${problemConjectureName.toString}, ${problemFormulas.size - 1} axioms.")
    }

    // process proof file, read to map, initialize conjecture name
    for (af <- proof.formulas) {
      af match {
        case f@TPTP.FOFAnnotated(name, _, _, _) =>
          proofFormulas += (name -> f)
          proofSteps = proofSteps :+ f
        case _ => throw new IllegalArgumentException("Only FOF input allowed at the moment.")
      }
    }

    //////////////////////////////////////////////////////////
    // Actual checking
    //////////////////////////////////////////////////////////
    // create/start check tasks. for first iteration just sequental.
    // TODO: refactor to switch to parallel check
    logger.fine("Processing proof ...")
    try {
      //////////////////
      // specific checks
      //////////////////
      // (I.1) does the proof end with false?
      checkProofEndsInFalse()
      // (I.2) are the annotated formulas names in the proof unique?
      checkFormulaNamesAreUnique()
      // (I.3) are the inference parents acyclic?
      checkInferencesAreAcyclic()
      // ... more?

      //////////////////
      // iteration over every proof line, check each line depending on its character:
      //////////////////
      /** All the steps that have already been processed. Initially empty. */
      var previousProofSteps: Seq[TPTP.FOFAnnotated] = Vector.empty
      for (proofstep <- proofSteps) {
        var addedNewFuture = false
        logger.finer(s"Checking proof step '${proofstep.name}' with annotation '${proofstep.annotations.map(_._1.pretty).getOrElse("")}' ...")
        logger.finer(proofstep.annotations.toString)
        //////
        // II. Checks that every line has to do regardless of their specific annotation
        //////
        // (II.1) check that role of proof step is admissible
        checkRole(proofstep)
        // ... more?

        //////
        // III. Checks that every line has to do individually, specific to their annotation
        //////
        val annotation = proofstep.annotations
        annotationType(annotation) match {
          case Some(annotationType) => annotationType match {
            case "inference" =>
              // (III.0) check that every inference parent (if any) actually exists (earlier in the proof)
              checkInferenceParentsExist(proofstep, previousProofSteps)
              val inferenceStatus0 = inferenceStatus(annotation)

              inferenceName(annotation) match {
                case Some(inference) => inference match {
                  // (III.1) if a "negated_conjecture" entry, does it correctly negate and has correct role?
                  case "negated_conjecture" if inferenceStatus0.contains(CTH) => checkNegatedInference(proofstep)
                  // (III.2) if a "skolemize" entry, does it correctly skolemize (use ASK)
                  case "skolemize" if inferenceStatus0.contains(ESA) => checkSkolemization(proofstep, configuration.relaxAnnotationFormat)
                  // (III.3) if generic status(thm)/status(cth) entry, does it follow from its parents? (using external ATPs)
                  case rule if inferenceStatus0.contains(THM) =>
                    checkGenericInference(rule, proofstep, Left(THM))
                    if (configuration.parallelism) addedNewFuture = true
                  case rule if inferenceStatus0.contains(CTH) =>
                    checkGenericInference(rule, proofstep, Right(CTH))
                    if (configuration.parallelism) addedNewFuture = true
                  case _ => // Error case: unknown inference rule with non-THM/CTH status
                    logger.severe(s"Unknown inference '$inference' with status '${inferenceStatus0.getOrElse("")}' in proof step '${proofstep.name}'.")
                    throw new VerificationFailedException(s"Proof step '${proofstep.name}' uses unknown inference rule '$inference' with non-thm/cth status ('${inferenceStatus0.getOrElse("")}') and cannot be checked.")
                }
                case None => // Unknown annotation, abort
                  logger.severe(s"Annotation of proof step '${proofstep.name}' unknown.")
                  throw new VerificationFailedException(s"Proof step '${proofstep.name}' uses malformed inference annotation.")
              }
            case "file" => if (configuration.problemPath.isDefined) checkFormulaFromFile(proofstep)
            case "introduced" => ??? // TODO
            case record => throw new VerificationFailedException(s"Proof step '${proofstep.name}' uses unknown record '$record'.")
          }
          case None => // no annotation is an error for all steps.
            logger.severe(s"Proof step '${proofstep.name}' has no or malformed annotation.")
            throw new VerificationFailedException(s"Proof step '${proofstep.name}' has no or malformed annotation.")
        }
        if (!configuration.parallelism || !addedNewFuture) logger.info(s"Check succeeded for step '${proofstep.name}'.") // Assuming we fail fast if anything happens before
        previousProofSteps = previousProofSteps :+ proofstep
      }


      if (configuration.parallelism) {
        // wait on completion of individual tasks
        // TODO from here, I guess?
        logger.info(s"Waiting for verification tasks to finish ...")
        Await.result(Future.sequence(openFutures), configuration.timeout.seconds)
        logger.info(s"All checks succeeded.")
      }

      // report success
      logger.info("Proof verified.")
      Verified
    } catch {
      case e: VerificationFailedException =>
        FailedVerified(e.getMessage)
      case e: VerificationTimedOutException =>
        NotVerified(e.getMessage)
      case _: concurrent.TimeoutException =>
        NotVerified("Timed out.")
    }
  }

  // TODO: We can add parallelism in these check methods below
  // What do parellize? I think probably just the generic thm steps, everything
  // else can be done internally and sequentially (quick fast, I think).
  private def checkProofEndsInFalse(): Unit = {
    logger.fine("Check for $false at the end of proof.")
    val endsWithFalseCheck = ProofEndsInFalseCheck.apply(proof)
    logger.info(s"Proof ends in $$false: $endsWithFalseCheck")
    if (!endsWithFalseCheck) throw new VerificationFailedException("Proof does not end in false")
  }

  private def checkFormulaNamesAreUnique(): Unit = {
    logger.fine("Check for uniqueness of formula names.")
    val namesUniqueCheck = FormulaNamesUniquenessCheck.apply(proofSteps)
    logger.info(s"Formula names unique: ${namesUniqueCheck.isEmpty}")
    if (namesUniqueCheck.isDefined) throw new VerificationFailedException(s"Name '${namesUniqueCheck.get}' is used multiple times.")
  }

  private def checkInferencesAreAcyclic(): Unit = {
    logger.fine("Check for acyclicity of inference parents.")
    val inferenceParentsAreAcyclic = InferenceParentsAcyclicityCheck.apply(proofSteps, proofFormulas,configuration.relaxAnnotationFormat)
    logger.info(s"Inference parents are acyclic: $inferenceParentsAreAcyclic")
    if (!inferenceParentsAreAcyclic) throw new VerificationFailedException("Graph of inference parents from $false contains a cycle.")
  }

  private def checkInferenceParentsExist(proofstep: TPTP.FOFAnnotated, previousProofSteps: Seq[TPTP.FOFAnnotated]): Unit = {
    logger.finer("Check for existence of inference parents (if any).")
    val inferenceParentsCheck = InferenceParentsExistCheck.apply(proofstep, previousProofSteps, configuration.relaxAnnotationFormat)
    logger.fine(s"Inference parents exist (${proofstep.name}): $inferenceParentsCheck")
    if (!inferenceParentsCheck) throw new VerificationFailedException(s"Proof step '${proofstep.name}' has unknown inference parents.")
  }

  private def checkRole(proofstep: TPTP.FOFAnnotated): Unit = {
    val allowedRoles = Seq("axiom", "conjecture", "negated_conjecture", "plain")
    val roleCheck = allowedRoles.contains(proofstep.role)
    if (!roleCheck) throw new VerificationFailedException(s"Proof step '${proofstep.name}' has unknown role.")
  }

  private def checkNegatedInference(proofstep: TPTP.FOFAnnotated): Unit = {
    logger.finer("Check for correct negation of conjecture")
    val checkNegation = ConjectureNegationCheck.apply(proofstep, problemConjectureName.flatMap(problemFormulas.get))
    logger.fine(s"Negation of conjecture correct (${proofstep.name}): $checkNegation")
    if (!checkNegation) throw new VerificationFailedException("Negation of conjecture is incorrect.")
  }

  private def checkSkolemization(proofstep: TPTP.FOFAnnotated, relaxAnnotationFormat: Boolean): Unit = {
    logger.finer("Check for correct skolemization")
    val checkSkolemize = SkolemizationCheck.apply(proofstep, proofFormulas, usedSkolemSymbols ++ problemSymbols, relaxAnnotationFormat)
    checkSkolemize match {
      case Left(msg) =>
        throw new VerificationFailedException(s"Skolemization in step '${proofstep.name}' is incorrect: $msg")
      case Right(skolemSymbolIntroduced) =>
        usedSkolemSymbols = usedSkolemSymbols + skolemSymbolIntroduced
    }
    logger.fine(s"Skolemization correct (${proofstep.name}): ${checkSkolemize.isRight}")

  }

  private def checkGenericInference(rule: String, proofstep: TPTP.FOFAnnotated, status: Either[THM.type , CTH.type]): Unit = {
    def run(): Unit = {
      logger.finer(s"Check for correct entailment of inference rule '$rule'.")
      val timeout = 30 // TODO: Timeout from somewhere
      val inferenceConfiguration = GenericInferenceCheck.InferenceConfig(configuration.eproverPath, timeout, configuration.relaxAnnotationFormat)
      val checkEntailment = GenericInferenceCheck.apply(proofstep, proofFormulas, status, inferenceConfiguration)
      checkEntailment match {
        case Some(check) =>
          logger.fine(s"Entailment correct (${proofstep.name}): $checkEntailment")
          if (!check) throw new VerificationFailedException(s"Proof step '${proofstep.name}' is not correct.")
        case None => throw new VerificationTimedOutException(s"Verification of proof step '${proofstep.name}' timed out.")
      }
    }
    if (configuration.parallelism) {
      val f = Future.apply(run())
      openFutures = openFutures :+ f
      logger.fine(s"Scheduled parallel inference check.")
    } else {
      run()
    }

  }

  private def checkFormulaFromFile(proofstep: TPTP.FOFAnnotated): Unit = {
    logger.finer("Check for correct premise usage from problem file.")
    assert(configuration.problemPath.isDefined)
    val problemFileName = configuration.problemPath.get.getFileName.toString
    val checkFormulaFromFile = CorrectFormulaFromFileCheck.apply(proofstep, problemFileName, problemFormulas)
    logger.fine(s"Formula equivalent to problem statement (${proofstep.name}): $checkFormulaFromFile")
    if (!checkFormulaFromFile) throw new VerificationFailedException(s"Proof step '${proofstep.name}' does not use correct formula from file.")
  }
}
object ProofCheckController {
  final case class Configuration(problemPath: Option[Path],
                                 proofPath: Path,
                                 timeout: Int,
                                 parallelism: Boolean,
                                 ignoreFileAnnotations: Boolean,
                                 relaxAnnotationFormat: Boolean,
                                 eproverPath: Path)

  /** Thrown during the check if some step yields that the proof definitely cannot be verified
   * because it's not a valid proof. */
  private class VerificationFailedException(msg: String) extends RuntimeException(msg)

  /** Thrown during the check by some step if it gives up (e.g. timeout of external prover). */
  private class VerificationTimedOutException(msg: String) extends RuntimeException(msg)

  private final val defaultTimeout: Int = 60
  private final val defaultParallel: Boolean = false
  private final val defaultIgnoreFileAnnotations: Boolean = false
  private final val defaultRelaxAnnotationFormat: Boolean = false
  // a bit hacky:
  private final val defaulteproverPath: Option[String] = scala.sys.process.Process("which eprover").lazyLines_!.headOption

  sealed abstract class Parameter

  final case class ProblemPath(path: Path) extends Parameter
  final case class Timeout(timeout: Int) extends Parameter
  final case object Parallelism extends Parameter

  final case object IgnoreFileAnnotations extends Parameter
  final case object RelaxAnnotationFormat extends Parameter
  final case class EproverPath(path: Path) extends Parameter

  /** Factory method for a [[ProofCheckController]] based on the given arguments. */
  final def apply(problemPath: Option[Path],
                  proofPath:  Path,
                  problem: Option[TPTP.Problem],
                  proof: TPTP.Problem,
                  parameters: Seq[ProofCheckController.Parameter]): Result = {
    var timeout = defaultTimeout
    var parallel = defaultParallel
    var ignoreFileAnnotations = defaultIgnoreFileAnnotations
    var relaxAnnotationFormat = defaultRelaxAnnotationFormat
    var path: Option[Path] = defaulteproverPath.map(p => Path.of(p))

    for (parameter <- parameters) {
      parameter match {
        case Timeout(to) => timeout = to
        case Parallelism => parallel = true
        case IgnoreFileAnnotations => ignoreFileAnnotations = true
        case RelaxAnnotationFormat => relaxAnnotationFormat = true
        case EproverPath(p) => path = Some(p)
      }
    }
    if (path.isEmpty) throw new IllegalArgumentException("eprover path unknown")
    val config = Configuration(problemPath, proofPath, timeout, parallel, ignoreFileAnnotations, relaxAnnotationFormat, path.get)
    val controller = new ProofCheckController(problem: Option[TPTP.Problem], proof: TPTP.Problem, config)
    controller.apply()
  }
}
