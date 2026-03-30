package noergler.checks

import leo.datastructures.TPTP
import leo.datastructures.TPTP.FOF
import noergler.checks.GenericInferenceCheck.logger
import noergler.{CTH, InferenceStatus, THM, proofStepParents}

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.file.Path
import java.util.logging.Logger

final class GenericInferenceCheck(premises: Seq[TPTP.FOFAnnotated],
                                  conjecture: TPTP.FOFAnnotated,
                                  eproverPath: Path,
                                  timeout: Int) {
  def apply(): Option[Boolean] = {
    // TODO: Ugly quick-shot implementation, just for testing. needs to be refactored.

    logger.finer(s"External prover check premises ${premises.map(_.pretty).mkString(", ")}.")
    logger.finer(s"External prover check conjecture ${conjecture.pretty}.")

    val eprocess = scala.sys.process.Process.apply(eproverPath.toString, Seq("-s", s"--cpu-limit=${timeout.toString}"))
    val problem = TPTP.Problem(Seq.empty, premises :+ conjecture, Map.empty)
    val response = eprocess.#<(new ByteArrayInputStream(problem.pretty.getBytes)).!! // TODO: redirect errorsteam to logger, TODO: catch exception if non-zero exit status
    logger.finest(s"E response: $response")
    if (response.contains("SZS status Theorem")) Some(true)
    else if (response.contains("SZS status ContradictoryAxioms")) Some(true)
    else Some(false)
  }

}
object GenericInferenceCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.Controller")



  final def apply(proofstep: TPTP.FOFAnnotated,
                  proofFormulas: Map[String, TPTP.FOFAnnotated],
                  status: Either[THM.type , CTH.type],
                  eproverPath: Path,
                  timeout: Int): Option[Boolean] = {
    val inferenceParentsNames: Option[Seq[String]] = proofStepParents(proofstep.annotations)
    inferenceParentsNames match {
      case Some(names) =>
        val inferenceParents = names.map(proofFormulas) // safe as we checked the existence of all parents before
        logger.finer(s"Inference parents: ${names.mkString(",")}")
        val premises = inferenceParents.map { af =>
          TPTP.FOFAnnotated(af.name, "axiom", af.formula, None)
        }
        val formulatoBeProved: TPTP.FOF.Statement = status match {
          case Left(_) => proofstep.formula
          case Right(_) => proofstep.formula match {
            case FOF.Logical(formula) => FOF.Logical(FOF.UnaryFormula(FOF.~, formula))
          }
        }
        val annotatedToBeProved: TPTP.FOFAnnotated = TPTP.FOFAnnotated("c", "conjecture", formulatoBeProved, None)
        new GenericInferenceCheck(premises, annotatedToBeProved, eproverPath, timeout).apply()
      case None =>
        logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
        Some(false)
    }
  }
}
