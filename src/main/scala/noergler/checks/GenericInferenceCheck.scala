package noergler.checks

import leo.datastructures.TPTP
import leo.datastructures.TPTP.FOF
import noergler.ProofCheckController.Prover
import noergler.checks.GenericInferenceCheck.logger
import noergler.{CTH, ProofCheckController, THM, proofStepParents}

import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process
import scala.sys.process.{ProcessLogger, Process => RunningProcess}

final class GenericInferenceCheck(premises: Seq[TPTP.FOFAnnotated],
                                  conjecture: TPTP.FOFAnnotated,
                                  prover: Prover,
                                  timeout: Int)
                                 (implicit ec: ExecutionContext) {

  /**
   * Start the prover process and return:
   *   - the running process handle
   *   - a Future that completes with the parsed prover result
   */
  def start(): (RunningProcess, Future[Option[Boolean]]) = {
    logger.finer(s"${prover.name} check premises ${premises.map(_.pretty).mkString(", ")}.")
    logger.finer(s"${prover.name} check conjecture ${conjecture.pretty}.")
    val problem = TPTP.Problem(Seq.empty, premises :+ conjecture, Map.empty)

    val stdout: StringBuffer = new StringBuffer()
    val stderr: StringBuffer = new StringBuffer()

    val proverProcess = prover match {
      case ProofCheckController.EProver(path) => run_eprover(path, problem)
      case ProofCheckController.Vampire(path) => run_vampire(path, problem)
    }

    val processLogger = ProcessLogger.apply(
      line => stdout.append(line),
      line => stderr.append(line)
    )

    val running: scala.sys.process.Process = proverProcess.run(processLogger)

    val resultFuture: Future[Option[Boolean]] = Future {
      val exitCode = running.exitValue() // blocks until process exits
      val errResult = stderr.toString
      logger.finest(s"${prover.name} stderr: $errResult")
      val result = stdout.toString
      logger.finest(s"${prover.name} exit code: $exitCode")
      logger.finest(s"${prover.name} response: $result")
      parse_TSTP_output(result)
    }

    (running, resultFuture)
  }

  def apply(): Option[Boolean] = {
    val (_, future) = start()
    scala.concurrent.Await.result(future, scala.concurrent.duration.Duration.Inf)
  }

  /**
   * Runs chosen prover on a given inference
   *
   * @return Some(true) if E returns ContradictoryAxioms or Theorem
   *         Some(false) if E returns CounterSatisfiable
   *         None otherwise (including timeout)
   */
  def apply_old(): Option[Boolean] = {
    logger.finer(s"${prover.name} check premises ${premises.map(_.pretty).mkString(", ")}.")
    logger.finer(s"${prover.name} check conjecture ${conjecture.pretty}.")
    val problem = TPTP.Problem(Seq.empty, premises :+ conjecture, Map.empty)

    val stdout: StringBuffer = new StringBuffer()
    val stderr: StringBuffer = new StringBuffer()

    val proverProcess = prover match {
      case ProofCheckController.EProver(path) => run_eprover(path, problem)
      case ProofCheckController.Vampire(path) => run_vampire(path, problem)
    }

    val processLogger = ProcessLogger.apply(
      line => stdout.append(line),
      line => stderr.append(line)
    )

    val running: scala.sys.process.Process = proverProcess.run(processLogger)

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
      Seq("--input_syntax","tptp","--proof","off","--avatar","off","--time_limit",timeout.toString)).#<(new ByteArrayInputStream(problem.pretty.getBytes))
  }

}
object GenericInferenceCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.Controller")

  final case class InferenceConfig( prover: Prover,
                                    timeout: Int,
                                    relaxAnnotationFormat: Boolean
                                  )

  final def constructInferenceProblem(proofstep: TPTP.FOFAnnotated,
                                      names: Seq[String],
                                      proofFormulas: Map[String, TPTP.FOFAnnotated],
                                      status: Either[THM.type , CTH.type])={
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
    (premises, annotatedToBeProved)
  }

  final def apply(proofstep: TPTP.FOFAnnotated,
                  proofFormulas: Map[String, TPTP.FOFAnnotated],
                  status: Either[THM.type , CTH.type],
                  config: InferenceConfig)
                 (implicit ec: ExecutionContext): Option[Boolean] = {
    val inferenceParentsNames: Option[Seq[String]] = proofStepParents(proofstep.annotations, config.relaxAnnotationFormat)
    inferenceParentsNames match {
      case Some(names) =>
        val (premises, annotatedToBeProved) = constructInferenceProblem(proofstep,names, proofFormulas, status)
        new GenericInferenceCheck(premises, annotatedToBeProved, config.prover, config.timeout).apply()
      case None =>
        logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
        Some(false)
    }
  }
}
