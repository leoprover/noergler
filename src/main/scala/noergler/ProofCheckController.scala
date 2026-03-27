package noergler

import leo.datastructures.TPTP
import noergler.ProofCheckController.{Configuration, NotVerified, Result, Verified, inferenceName, inferenceParents, inferenceStatus}
import noergler.checks.{ConjectureNegationCheck, CorrectPremiseFromFileCheck, FormulaNamesUniquenessCheck, GenericTHMInferenceCheck, InferenceParentsExistCheck, ProofEndsInFalseCheck, SkolemizationCheck}

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
    // TODO
    // (I.n) ... more?

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
          val checkPremise = CorrectPremiseFromFileCheck.apply(proofstep, problemFormulas)
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
      proofstep.annotations match {
        case Some(annotation) =>
          inferenceName(annotation) match {
            case Some(inference) => inference match {
              case "negated_conjecture" =>
                // (III.1) if a "negated_conjecture" entry, does it correctly negate and has correct role?
                logger.finer("Check for correct negation of conjecture")
                val checkNegation = ConjectureNegationCheck.apply(proofstep, problemConjectureName.flatMap(problemFormulas.get))
                // TODO: Handle negative result somehow, failed verified
                logger.fine(s"Negation of conjecture correct (${proofstep.name}): $checkNegation")
              case "skolemize" =>
                // (III.2) if a "skolemization" entry, does it correctly skolemize, use ASK
                logger.finer("Check for correct skolemization")
                val checkSkolemize = SkolemizationCheck.apply(proofstep, ???) // TODO
                // TODO: Handle negative result somehow, failed verified
                logger.fine(s"Skolemization correct (${proofstep.name}): $checkSkolemize")
              case rule if inferenceStatus(annotation).getOrElse("") == "thm" =>
                // (III.3) if generic status(thm) entry, does it follow from its parents? (using external ATPs)
                logger.finer(s"Check for correct entailment of inference rule '$rule'.")
                val inferenceParentsNames: Option[Seq[String]] = inferenceParents(annotation) // TODO
                inferenceParentsNames match {
                  case Some(names) =>
                    val inferenceParents = names.map(proofFormulas) // safe as we checked the existence of all parents before
                    logger.finer(s"Inference parents: ${names.mkString(",")}")
                    val checkEntailment = GenericTHMInferenceCheck.apply(proofstep, inferenceParents)
                    // TODO: Handle negative result somehow, failed verified
                    logger.fine(s"Entailment correct (${proofstep.name}): $checkEntailment")
                  case None =>
                    logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
                  // TODO: Handle error somehow, failed verified
                }

              case _ => // Error case: unknown inference rule with non-THM status
                logger.severe(s"Unknown inference '$inference' with non-thm status in proof step '${proofstep.name}'.")
                // TODO: Handle error someshow, failed verified/not verified?
            }
            case None => // Unknown annotation, abort
              logger.severe(s"Annotation of proof step '${proofstep.name}' unknown.")
              // TODO: Handle error someshow, failed verified
          }
        // no annotation is an error for "plain" and "negated_conjecture" steps
        case None if Seq("plain", "negated_conjecture").contains(proofstep.role) =>
          logger.severe(s"Proof step '${proofstep.name}' has 'plain' role but no annotation.")
        // TODO: Handle somehow
        case None => () // do nothing. TODO: Check that axioms/conjecture never has annotations?
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
}
object ProofCheckController {
  final case class Configuration(timeout: Int,
                                 coreCount: Int)

  final val defaultTimeout: Int = 60
  final val defaultCoreCount: Int = 1

  sealed abstract class Parameter
  final case class Timeout(timeout: Int) extends Parameter
  final case class Cores(coreCount: Int) extends Parameter

  sealed abstract class Result
  final case object Verified extends Result
  final case class FailedVerified(reason: String) extends Result
  final case class NotVerified(reason: String) extends Result

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

  final def inferenceName(annotation: (TPTP.GeneralTerm, Option[Seq[TPTP.GeneralTerm]])): Option[String] = {
    val gt = annotation._1
    if (gt.data.nonEmpty) {
      gt.data.head match {
        case TPTP.MetaFunctionData("inference", args) if args.nonEmpty => args.head.data match {
          case Seq(TPTP.MetaFunctionData(inferenceName, Seq())) => Some(inferenceName)
          case _ => None
        }
        case _ => None
      }
    } else None
  }
  final def inferenceStatus(annotation: (TPTP.GeneralTerm, Option[Seq[TPTP.GeneralTerm]])): Option[String] = {
    val gt = annotation._1
    if (gt.data.nonEmpty) {
      gt.data.head match {
        case TPTP.MetaFunctionData("inference", args) if args.size >= 2 => args.tail.head.list match {
          case Some(Seq(gt0)) => gt0.data match {
            case Seq(TPTP.MetaFunctionData("status", Seq(gt1))) => gt1.data match {
              case Seq(TPTP.MetaFunctionData(status, Seq())) => Some(status)
              case _ => None
            }
            case _ => None
          }
          case _ => None
        }
        case _ => None
      }
    } else None
  }
  /** Returns a list of inference parents' names, if any.
   * Empty list of parents is returned as Some(Seq()), None is an error case. */
  final def inferenceParents(annotation: (TPTP.GeneralTerm, Option[Seq[TPTP.GeneralTerm]])): Option[Seq[String]] = {
    val gt = annotation._1
    if (gt.data.nonEmpty) {
      gt.data.head match {
        case TPTP.MetaFunctionData("inference", args) if args.size >= 3 => args.tail.tail.head.list match {
          case Some(parentsAnnotation) =>
            val result = parentsAnnotation.flatMap(inferenceParent0)
            if (result.size == parentsAnnotation.size) Some(result)
            else None // Not well-formed
          case _ => None
        }
        case _ => None
      }
    } else None
  }
  private final def inferenceParent0(parentAnnotation: TPTP.GeneralTerm): Option[String] = {
    if (parentAnnotation.list.isDefined) None
    else {
      parentAnnotation.data match {
        case Seq(TPTP.MetaFunctionData(parentName, Seq())) => Some(parentName)
        case _ => None
      }
    }
  }
}
