package noergler

import leo.datastructures.TPTP
import noergler.ProofCheckController.{Configuration, VerificationFailedException}
import noergler.checks.{ConjectureNegationCheck, CorrectFormulaFromFileCheck, FormulaNamesUniquenessCheck, GenericTHMInferenceCheck, InferenceParentsAcyclicityCheck, InferenceParentsExistCheck, ProofEndsInFalseCheck, SkolemizationCheck}

import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, Future}

/**
 * The Controller coordinates the checking process, i.e., how and when
 * which checks are started etc (in parallel oder sequential).
 *
 * @param problem The source problem
 * @param proof The proof to be checked
 * @param configuration The checking configuration
 */
class ProofCheckController(problem: TPTP.Problem,
                           proof: TPTP.Problem,
                           configuration: Configuration) {
  final val logger: Logger = Logger.getLogger("Nörgler.Controller")
//  final private def coreCountToThreadCount(coreCount: Int): Int = coreCount
//  final implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(
//    Executors.newFixedThreadPool(coreCountToThreadCount(configuration.coreCount)))

  /** Map of problem file TPTP annotated formula name (hopefully unique) -> the formula */
  private var problemFormulas: Map[String, TPTP.FOFAnnotated] = Map.empty
  /** Name of the conjecture from the problem file, if known  */
  private var problemConjectureName: Option[String] = None

  /** The sequence of proof steps as they appear in the proof. */
  private var proofSteps: Seq[TPTP.FOFAnnotated] = Vector.empty
  /** Map of proof file TPTP annotated formula name (hopefully unique) -> the formula */
  private var proofFormulas: Map[String, TPTP.FOFAnnotated] = Map.empty

  final def apply(): Result = {
    //////////////////////////////////////////////////////////
    // preliminary steps
    //////////////////////////////////////////////////////////
    logger.config(s"Used configuration: ${configuration.toString}")
    logger.fine("Processing problem file ...")
    // process problem file, read to map, initialize conjecture name
    for (af <- problem.formulas) {
      af match {
        case f@TPTP.FOFAnnotated(name, "conjecture", _, _) =>
          problemFormulas += (name -> f)
          problemConjectureName = Some(name)
        case f@TPTP.FOFAnnotated(name, _, _, _) =>
          problemFormulas += (name -> f)
        case _ => throw new IllegalArgumentException("Only FOF input allowed at the moment.")
      }
    }
    logger.info(s"Conjecture in problem found: ${problemConjectureName.toString}, ${problemFormulas.size-1} axioms.")

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
    // create/start check tasks. for first iteration maybe just sequential?
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

              inferenceName(annotation) match {
                case Some(inference) => inference match {
                  // (III.1) if a "negated_conjecture" entry, does it correctly negate and has correct role?
                  case "negated_conjecture" => checkNegatedInference(proofstep)
                  // (III.2) if a "skolemize" entry, does it correctly skolemize (use ASK)
                  case "skolemize" => checkSkolemization(proofstep)
                  // (III.3) if generic status(thm) entry, does it follow from its parents? (using external ATPs)
                  case rule if inferenceStatus(annotation).getOrElse("") == "thm" => checkTHMInference(rule, proofstep)
                  case _ => // Error case: unknown inference rule with non-THM status
                    logger.severe(s"Unknown inference '$inference' with non-thm status in proof step '${proofstep.name}'.")
                    throw new VerificationFailedException(s"Proof step '${proofstep.name}' uses unknown inference rule '$inference' with non-thm status, cannot be checked.")
                }
                case None => // Unknown annotation, abort
                  logger.severe(s"Annotation of proof step '${proofstep.name}' unknown.")
                  throw new VerificationFailedException(s"Proof step '${proofstep.name}' uses malformed inference annotation.")
              }
            case "file" => checkFormulaFromFile(proofstep)
            case "introduced" => ??? // TODO
            case record => throw new VerificationFailedException(s"Proof step '${proofstep.name}' uses unknown record '$record'.")
          }
          case None => // no annotation is an error for all steps.
            logger.severe(s"Proof step '${proofstep.name}' has no or malformed annotation.")
            throw new VerificationFailedException(s"Proof step '${proofstep.name}' has no or malformed annotation.")
        }
        logger.info(s"Check succeeded for step '${proofstep.name}'.") // Assuming we fail fast if anything happens before
        previousProofSteps = previousProofSteps :+ proofstep
      }

      // TODO from here
      // wait on completion of individual tasks
      // how? probably just in the order above, as they must finish all anyway for
      // it to be a success
      // if some on the way fail -> fail
      // if some on the way timeout -> ???
      // if none fails -> success

      // report success
      Verified
    } catch {
      case e: VerificationFailedException =>
        FailedVerified(e.getMessage)
    }
  }

  // TODO: We can add parallelism in these check methods below
  // What do parellize? I think probably just the generic thm steps, everything
  // else can be done internally and sequentially (quick fast, I think).
  private def checkProofEndsInFalse(): Unit = {
    logger.fine("Check for $false at the end of proof.")
    val endsWithFalseCheck = ProofEndsInFalseCheck.apply(proof)
    logger.info(s"Proof ends in $$false: $endsWithFalseCheck")
    if (!endsWithFalseCheck) throw new VerificationFailedException("Proof does not end it false")
  }

  private def checkFormulaNamesAreUnique(): Unit = {
    logger.fine("Check for uniqueness of formula names.")
    val namesUniqueCheck = FormulaNamesUniquenessCheck.apply(proofSteps)
    logger.info(s"Formula names unique: $namesUniqueCheck")
    if (!namesUniqueCheck) throw new VerificationFailedException("Names of annotated formulas in proof are not unique.")
  }

  private def checkInferencesAreAcyclic(): Unit = {
    logger.fine("Check for acyclicity of inference parents.")
    val inferenceParentsAreAcyclic = InferenceParentsAcyclicityCheck.apply(proofSteps, proofFormulas)
    logger.info(s"Inference parents are acyclic: $inferenceParentsAreAcyclic")
    if (!inferenceParentsAreAcyclic) throw new VerificationFailedException("Graph of inference parents from $false contains a cycle.")
  }

  private def checkInferenceParentsExist(proofstep: TPTP.FOFAnnotated, previousProofSteps: Seq[TPTP.FOFAnnotated]): Unit = {
    logger.finer("Check for existence of inference parents (if any).")
    val inferenceParentsCheck = InferenceParentsExistCheck.apply(proofstep, previousProofSteps)
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

  private def checkSkolemization(proofstep: TPTP.FOFAnnotated): Unit = {
    logger.finer("Check for correct skolemization")
    val checkSkolemize = SkolemizationCheck.apply(proofstep, ???) // TODO
    logger.fine(s"Skolemization correct (${proofstep.name}): $checkSkolemize")
    if (!checkSkolemize) throw new VerificationFailedException(s"Skolemization in step '${proofstep.name}' is incorrect.")
  }

  private def checkTHMInference(rule: String, proofstep: TPTP.FOFAnnotated): Unit = {
    logger.finer(s"Check for correct entailment of inference rule '$rule'.")
    val checkEntailment = GenericTHMInferenceCheck.apply(proofstep, proofFormulas)
    // TODO: how to handle timeouts? Maybe Option[Boolean] instead of Boolean and None is for timeout?
    //  So we can reschedule or so?
    logger.fine(s"Entailment correct (${proofstep.name}): $checkEntailment")
    if (!checkEntailment) throw new VerificationFailedException(s"Proof step '${proofstep.name}' cannot be verified.")
  }

  private def checkFormulaFromFile(proofstep: TPTP.FOFAnnotated): Unit = {
    logger.finer("Check for correct premise usage from problem file.")
    val problemFileName = configuration.problemPath.getFileName.toString
    val checkFormulaFromFile = CorrectFormulaFromFileCheck.apply(proofstep, problemFileName, problemFormulas)
    logger.fine(s"Formula equivalent to problem statement (${proofstep.name}): $checkFormulaFromFile")
    if (!checkFormulaFromFile) throw new VerificationFailedException(s"Proof step '${proofstep.name}' does not use correct formula from file.")
  }
}
object ProofCheckController {
  final case class Configuration(problemPath: Path,
                                 proofPath: Path,
                                 timeout: Int,
                                 coreCount: Int)

  /** Thrown during the check if some step yields that the proof definitely cannot be verified
   * because it's not a valid proof. */
  private class VerificationFailedException(msg: String) extends RuntimeException(msg)
  /** Thrown during the check by some step if it gives up (e.g. timeout of external prover). */
  private class NotVerifiedException(msg: String) extends RuntimeException(msg)

  private final val defaultTimeout: Int = 60
  private final val defaultCoreCount: Int = 1

  sealed abstract class Parameter
  final case class Timeout(timeout: Int) extends Parameter
  final case class Cores(coreCount: Int) extends Parameter

  /** Factory method for a [[ProofCheckController]] based on the given arguments. */
  final def apply(problemPath: Path,
                  proofPath:  Path,
                  problem: TPTP.Problem,
                  proof: TPTP.Problem,
                  parameters: Seq[ProofCheckController.Parameter]): Result = {
    var timeout = defaultTimeout
    var coreCount = defaultCoreCount
    for (parameter <- parameters) {
      parameter match {
        case Timeout(to) => timeout = to
        case Cores(cc) => coreCount = cc
      }
    }
    val config = Configuration(problemPath, proofPath, timeout, coreCount)
    val controller = new ProofCheckController(problem: TPTP.Problem, proof: TPTP.Problem, config)
    controller.apply()
  }
}
