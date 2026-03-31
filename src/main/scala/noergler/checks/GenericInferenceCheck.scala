package noergler.checks

import leo.datastructures.TPTP
import leo.datastructures.TPTP.FOF
import noergler.checks.GenericInferenceCheck.logger
import noergler.{CTH, THM, proofStepParents}

import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.logging.Logger
import scala.sys.process.ProcessLogger

final class GenericInferenceCheck(premises: Seq[TPTP.FOFAnnotated],
                                  conjecture: TPTP.FOFAnnotated,
                                  eproverPath: Path,
                                  timeout: Int) {
  def apply(): Option[Boolean] = {
    // TODO: Ugly quick-shot implementation, just for testing. needs to be refactored.

    logger.finer(s"External prover check premises ${premises.map(_.pretty).mkString(", ")}.")
    logger.finer(s"External prover check conjecture ${conjecture.pretty}.")
    val problem = TPTP.Problem(Seq.empty, premises :+ conjecture, Map.empty)

    val stdout: StringBuffer = new StringBuffer()
    val stderr: StringBuffer = new StringBuffer()
    val eprocess = scala.sys.process.Process.apply(
      eproverPath.toString,
      Seq("-s", "--auto", s"--cpu-limit=${timeout.toString}")).#<(new ByteArrayInputStream(problem.pretty.getBytes))
    val processLogger = ProcessLogger.apply(
      line => stdout.append(line),
      line => stderr.append(line)
    )
    val _ = eprocess ! processLogger // exit code is dont-care

    val result = stdout.toString
    val errResult = stderr.toString
    logger.finest(s"E response: $result")
    logger.finest(s"E stderr: $errResult")
    // TODO: Improve handling below
    if (result.contains("SZS status Theorem")) Some(true)
    else if (result.contains("SZS status ContradictoryAxioms")) Some(true)
    else if (result.contains("SZS status CounterSatisfiable")) Some(false)
    else if (result.contains("SZS status GaveUp")) None
    else if (result.contains("SZS status Unknown")) None
    else None
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
