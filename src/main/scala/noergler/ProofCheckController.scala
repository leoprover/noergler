package noergler

import leo.datastructures.TPTP
import noergler.ProofCheckController.Configuration
import noergler.checks.{ConjectureNegationCheck, CorrectFormulaFromFileCheck, FormulaNamesUniquenessCheck, GenericTHMInferenceCheck, InferenceParentsAcyclicityCheck, InferenceParentsExistCheck, ProofEndsInFalseCheck, SkolemizationCheck}

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
  // TODO: What do parellize? I think probably just the generic thm steps, everything
  // else can be done internally and sequentially (quick fast, I think).

  /** Map of problem file TPTP annotated formula name (hopefully unique) -> the formula */
  private var problemFormulas: Map[String, TPTP.FOFAnnotated] = Map.empty
  /** Name of the conjecture from the problem file, if known  */
  private var problemConjectureName: Option[String] = None

  /** The sequence of proof steps as they appear in the proof. */
  private var proofSteps: Seq[TPTP.FOFAnnotated] = Seq.empty
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
    /* TODO: The results of the proof checking are not yet collected.
       But since it is planned to use Futures or similar to execute, maybe
       we just skip a principled approach here and manually check for now */
    //////////////////
    // specific checks
    //////////////////
    // (I.1) does the proof end with false?
    logger.fine("Check for $false at the end of proof.")
    val endsWithFalseCheck = ProofEndsInFalseCheck.apply(proof)
    // TODO: Handle negative result somehow, failed verified
    logger.info(s"Proof ends in $$false: $endsWithFalseCheck")
    // (I.2) are the annotated formulas names in the proof unique?
    logger.fine("Check for uniqueness of formula names.")
    val namesUniqueCheck = FormulaNamesUniquenessCheck.apply(proofSteps)
    // TODO: Handle negative result somehow, failed verified
    logger.info(s"Formula names unique: $namesUniqueCheck")
    // (I.3) are the inference parents acyclic?
    logger.fine("Check for acyclicity of inference parents.")
    val inferenceParentsAreAcyclic = InferenceParentsAcyclicityCheck.apply(proofSteps, proofFormulas)
    logger.info(s"Inference parents are acyclic: $inferenceParentsAreAcyclic")
    // TODO: Handle negative result somehow, failed verified
    // ... more checks here?

    //////////////////
    // iteration over every proof line, check each line depending on its character:
    //////////////////
    for (proofstep <- proofSteps) {
      logger.finer(s"Checking proof step '${proofstep.name}' with annotation '${proofstep.annotations.map(_._1.pretty).getOrElse("")}' ...")
      // II. Checks that every line has to do regardless of their specific annotation
      // (II.1) check that every inference parent (if any) actually exists (earlier in the proof)
      logger.finer("Check for existence of inference parents (if any).")
      val inferenceParentsCheck = InferenceParentsExistCheck.apply(proofstep, proofSteps)
      // TODO: Handle negative result somehow, failed verified
      logger.fine(s"Inference parents exist (${proofstep.name}): $inferenceParentsCheck")

      // (II.2) if it is a line copied from the problem (axiom or conjecture), check that it is corectly copied
      proofstep.role match {
        case "conjecture" | "axiom" =>
          logger.finer("Check for correct premise usage from problem.")
          val checkPremise = CorrectFormulaFromFileCheck.apply(proofstep, problemFormulas)
          // TODO: Handle negative result somehow, failed verified
          logger.fine(s"Formula equivalent to problem statement (${proofstep.name}): $checkPremise")
        case "negated_conjecture" => () // Nothing to do
        case "plain" => () // Nothing to do
        case role => // Unknown role, error case
          logger.severe(s"Unknown proof step role '$role'. Proof malformed.")
          // TODO: Handle error somehow, failed verified
      }

      //////
      // III. Checks that every line has to do individually, specific to their annotation
      val annotation = proofstep.annotations
      annotationType(annotation) match {
        case Some(annotationType) => annotationType match {
          case "inference" =>
            inferenceName(annotation) match {
              case Some(inference) => inference match {
                case "negated_conjecture" =>
                  // (III.1) if a "negated_conjecture" entry, does it correctly negate and has correct role?
                  logger.finer("Check for correct negation of conjecture")
                  val checkNegation = ConjectureNegationCheck.apply(proofstep, problemConjectureName.flatMap(problemFormulas.get))
                  // TODO: Handle negative result somehow, failed verified
                  logger.fine(s"Negation of conjecture correct (${proofstep.name}): $checkNegation")

                // (III.2) if a "skolemize" entry, does it correctly skolemize (use ASK)
                case "skolemize" => checkSkolemization(proofstep)
                  // TODO: Handle negative result somehow, failed verified

                // (III.3) if generic status(thm) entry, does it follow from its parents? (using external ATPs)
                case rule if inferenceStatus(annotation).getOrElse("") == "thm" =>

                  logger.finer(s"Check for correct entailment of inference rule '$rule'.")
                  val checkEntailment = GenericTHMInferenceCheck.apply(proofstep, proofFormulas)
                  // TODO: Handle negative result somehow, failed verified
                  logger.fine(s"Entailment correct (${proofstep.name}): $checkEntailment")

                case _ => // Error case: unknown inference rule with non-THM status
                  logger.severe(s"Unknown inference '$inference' with non-thm status in proof step '${proofstep.name}'.")
                // TODO: Handle error someshow, failed verified/not verified?
              }
              case None => // Unknown annotation, abort
                logger.severe(s"Annotation of proof step '${proofstep.name}' unknown.")
              // TODO: Handle error someshow, failed verified
            }

          case "file" =>
            logger.finer("Check for correct premise usage from problem file.")
            val checkPremise = CorrectFormulaFromFileCheck.apply(proofstep, problemFormulas)
            // TODO: Handle negative result somehow, failed verified
            logger.fine(s"Formula equivalent to problem statement (${proofstep.name}): $checkPremise")

          case _ => ???
        }
        case None =>
          // no annotation is an error for all steps.
          logger.severe(s"Proof step '${proofstep.name}' has no or malformed annotation.")
          // TODO: Handle somehow
      }
      logger.info(s"Check succeeded for step '${proofstep.name}'.") // Assuming we fail fast if anything happens before
    }

    // wait on completion of individual tasks
    // how? probably just in the order above, as they must finish all anyway for
    // it to be a success
    // if some on the way fail -> fail
    // if some on the way timeout -> ???
    // if none fails -> success

    // report success
    Verified

    NotVerified("gave up") // for now
  }

  private def checkSkolemization(proofstep: TPTP.FOFAnnotated): Boolean = {
    logger.finer("Check for correct skolemization")
    val checkSkolemize = SkolemizationCheck.apply(proofstep, ???) // TODO
    logger.fine(s"Skolemization correct (${proofstep.name}): $checkSkolemize")
    checkSkolemize
  }
}
object ProofCheckController {
  final case class Configuration(timeout: Int,
                                 coreCount: Int)

  private final val defaultTimeout: Int = 60
  private final val defaultCoreCount: Int = 1

  sealed abstract class Parameter
  final case class Timeout(timeout: Int) extends Parameter
  final case class Cores(coreCount: Int) extends Parameter

  /** Factory method for a [[ProofCheckController]] based on the given arguments. */
  final def apply(problem: TPTP.Problem, proof: TPTP.Problem, parameters: Seq[ProofCheckController.Parameter]): Result = {
    var timeout = defaultTimeout
    var coreCount = defaultCoreCount
    for (parameter <- parameters) {
      parameter match {
        case Timeout(to) => timeout = to
        case Cores(cc) => coreCount = cc
      }
    }
    val config = Configuration(timeout, coreCount)
    val controller = new ProofCheckController(problem: TPTP.Problem, proof: TPTP.Problem, config)
    controller.apply()
  }
}
