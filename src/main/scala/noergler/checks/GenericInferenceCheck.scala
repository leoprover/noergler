package noergler.checks

import leo.datastructures.TPTP
import leo.datastructures.TPTP.FOF
import noergler.ProofCheckController.Prover
import noergler.checks.GenericInferenceCheck.logger
import noergler.{CTH, ProofCheckController, THM, proofStepParents}

import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.logging.Logger
import scala.sys.process
import scala.sys.process.ProcessLogger

final class GenericInferenceCheck(premises: Seq[TPTP.FOFAnnotated],
                                  conjecture: TPTP.FOFAnnotated,
                                  prover: Prover,
                                  timeout: Int) {

  /**
   * Runs chosen prover on a given inference
   *
   * @return Some(true) if E returns ContradictoryAxioms or Theorem
   *         Some(false) if E returns CounterSatisfiable
   *         None otherwise (including timeout)
   */
  def apply(): Option[Boolean] = {
    logger.finer(s"${prover.name} check premises ${premises.map(_.pretty).mkString(", ")}.")
    logger.finer(s"${prover.name} check conjecture ${conjecture.pretty}.")
    val problem = TPTP.Problem(Seq.empty, premises :+ conjecture, Map.empty)

    val stdout: StringBuffer = new StringBuffer()
    val stderr: StringBuffer = new StringBuffer()

    val process = prover match {
      case ProofCheckController.EProver(path) => run_eprover(path, problem)
      case ProofCheckController.Vampire(path) => run_vampire(path, problem)
    }

    val processLogger = ProcessLogger.apply(
      line => stdout.append(line),
      line => stderr.append(line)
    )
    val _ = process ! processLogger // exit code is dont-care

    val errResult = stderr.toString
    logger.finest(s"${prover.name} stderr: $errResult")
    val result = stdout.toString
    logger.finest(s"${prover.name} response: $result")
    parse_TSTP_output(result)
  }

  def parse_TSTP_output(result: String): Option[Boolean] = {
    // TODO: Improve handling below
    if (result.contains("SZS status Theorem")) Some(true)
    else if (result.contains("SZS status ContradictoryAxioms")) Some(true)
    else if (result.contains("SZS status CounterSatisfiable")) Some(false)
    else if (result.contains("SZS status GaveUp")) None
    else if (result.contains("SZS status Unknown")) None
    else None
  }

  def run_eprover(eproverPath: Path, problem: TPTP.Problem): process.ProcessBuilder = {
    // TODO: Ugly quick-shot implementation, just for testing. needs to be refactored.
    scala.sys.process.Process.apply(
      eproverPath.toString,
      Seq("-s", "--auto", s"--cpu-limit=${timeout.toString}")).#<(new ByteArrayInputStream(problem.pretty.getBytes))
  }

  def run_vampire(vampirePath: Path, problem: TPTP.Problem): process.ProcessBuilder = {
    // TODO: Ugly quick-shot implementation, just for testing. needs to be refactored.
    scala.sys.process.Process.apply(
      vampirePath.toString,
      Seq("--input_syntax","tptp","--time_limit",timeout.toString)).#<(new ByteArrayInputStream(problem.pretty.getBytes))
  }

}
object GenericInferenceCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.Controller")

  final case class InferenceConfig( prover: Prover,
                                    timeout: Int,
                                    relaxAnnotationFormat: Boolean
                                  )


  final def apply(proofstep: TPTP.FOFAnnotated,
                  proofFormulas: Map[String, TPTP.FOFAnnotated],
                  status: Either[THM.type , CTH.type],
                  config: InferenceConfig): Option[Boolean] = {
    val inferenceParentsNames: Option[Seq[String]] = proofStepParents(proofstep.annotations, config.relaxAnnotationFormat)
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
        new GenericInferenceCheck(premises, annotatedToBeProved, config.prover, config.timeout).apply()
      case None =>
        logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
        Some(false)
    }
  }
}
