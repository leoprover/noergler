package noergler.checks

import leo.datastructures.TPTP
import noergler.checks.GenericInferenceCheck.InferenceStatus
import noergler.proofStepParents

import java.util.logging.Logger

final class GenericInferenceCheck(status: InferenceStatus,
                                  proofStep: TPTP.FOFAnnotated,
                                  inferenceParents: Seq[TPTP.FOFAnnotated]) {
  def apply(): Boolean = {
    false
  }
}
object GenericInferenceCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.Controller")

  sealed abstract class InferenceStatus
  final case object THM extends InferenceStatus
  final case object CTH extends InferenceStatus

  final def apply(proofstep: TPTP.FOFAnnotated,
                  proofFormulas: Map[String, TPTP.FOFAnnotated]): Option[Boolean] = {
    val inferenceParentsNames: Option[Seq[String]] = proofStepParents(proofstep.annotations)
    inferenceParentsNames match {
      case Some(names) =>
        val inferenceParents = names.map(proofFormulas) // safe as we checked the existence of all parents before
        logger.finer(s"Inference parents: ${names.mkString(",")}")
        Some(new GenericInferenceCheck(???, proofstep, inferenceParents).apply()) // TODO
      case None =>
        logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
        Some(false)
    }
  }
}
