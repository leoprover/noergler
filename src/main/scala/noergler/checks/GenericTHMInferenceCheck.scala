package noergler.checks

import leo.datastructures.TPTP
import noergler.proofStepParents

import java.util.logging.Logger

final class GenericTHMInferenceCheck(proofStep: TPTP.FOFAnnotated,
                               inferenceParents: Seq[TPTP.FOFAnnotated]) {
  def apply(): Boolean = {
    false
  }
}
object GenericTHMInferenceCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.Controller")

  final def apply(proofstep: TPTP.FOFAnnotated,
                  proofFormulas: Map[String, TPTP.FOFAnnotated]): Boolean = {
    val inferenceParentsNames: Option[Seq[String]] = proofStepParents(proofstep.annotations)
    inferenceParentsNames match {
      case Some(names) =>
        val inferenceParents = names.map(proofFormulas) // safe as we checked the existence of all parents before
        logger.finer(s"Inference parents: ${names.mkString(",")}")
        new GenericTHMInferenceCheck(proofstep, inferenceParents).apply()
      case None =>
        logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
        false
    }
  }
}
