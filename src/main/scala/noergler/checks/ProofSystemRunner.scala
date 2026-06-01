package noergler.checks

import leo.datastructures.TPTP
import noergler.ProofCheckController
import noergler.ProofCheckController.{ModelFinder, ProofSystem, Prover}
import noergler.checks.GenericInferenceCheck.logger

import java.io.ByteArrayInputStream
import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.sys.process
import scala.sys.process.{ProcessLogger, Process => RunningProcess}

final class ProofSystemRunner(premises: Seq[TPTP.AnnotatedFormula],
                              conjecture: TPTP.AnnotatedFormula,
                              system: ProofSystem,
                              timeout: Int)
                             (implicit externalSystemEc: ExecutionContext){
  import ProofSystemRunner.RunningProver

  // todo: we probably do not want to spend as much time on model finding, but what is a sensible output?
  private val modelFinderTimeout: Int = math.max(1, timeout / 3)

  def apply(): (RunningProcess, Future[Option[Boolean]]) = {
    val started = ProofSystemRunner(premises, conjecture, system, timeout)(externalSystemEc).startProcess()
    val resultFuture = Future {
      blocking{collectResult(started)}
    }(externalSystemEc)
    (started.process, resultFuture)
  }

  private def startProcess(): RunningProver = {
    val uniquePremises = premises.distinct
    logger.finer(s"${system.name} check premises ${uniquePremises.map(_.pretty).mkString(", ")}.")
    logger.finer(s"${system.name} check conjecture ${conjecture.pretty}.")
    val problem = TPTP.Problem(Seq.empty, uniquePremises :+ conjecture, Map.empty)

    val stdout: StringBuffer = new StringBuffer()
    val stderr: StringBuffer = new StringBuffer()

    val proverProcess = system match {
      case prover: Prover =>
        prover match {
          case ProofCheckController.EProver(path) => run_eprover(path, problem)
          case ProofCheckController.Vampire(path) => run_vampire(path, problem)
        }
      case finder: ModelFinder =>
        finder match {
          case ProofCheckController.Mace4(path) => run_mace4(path, problem)
        }
    }

    val processLogger = ProcessLogger.apply(
      line => stdout.append(line),
      line => stderr.append(line)
    )

    val running: scala.sys.process.Process = proverProcess.run(processLogger)
    RunningProver(running, stdout, stderr)
  }

  private def collectResult(started: RunningProver): Option[Boolean] = {
    try {
      val exitCode = started.process.exitValue() // blocks until process exits
      val errResult = started.stderr.toString
      logger.finest(s"${system.name} stderr: $errResult")
      val result = started.stdout.toString
      logger.finest(s"${system.name} exit code: $exitCode")
      logger.finest(s"${system.name} response: $result")
      parse_TSTP_output(result)
    } catch {
      // if we destroy running processes, we need to catch the cases
      case ex: Throwable =>
        logger.fine(s"${system.name} result collection failed: ${ex.getMessage}")
        None
    }
  }

  private def parse_TSTP_output(result: String): Option[Boolean] = {
    // TODO: Improve handling below
    if (result.contains("SZS status Theorem")) Some(true)
    else if (result.contains("SZS status ContradictoryAxioms")) Some(true)
    else if (result.contains("SZS status CounterSatisfiable")) Some(false)
    else if (result.contains("SZS status GaveUp")) None
    else if (result.contains("SZS status Unknown")) None
    else None
  }

  private def run_eprover(eproverPath: Path, problem: TPTP.Problem): process.ProcessBuilder = {
    // TODO: Ugly quick-shot implementation, just for testing. needs to be refactored.
    scala.sys.process.Process.apply(
      eproverPath.toString,
      Seq("-s", "--auto", s"--cpu-limit=${timeout.toString}")).#<(new ByteArrayInputStream(problem.pretty.getBytes))
  }

  private def run_vampire(vampirePath: Path, problem: TPTP.Problem): process.ProcessBuilder = {
    // TODO: Ugly quick-shot implementation, just for testing. needs to be refactored.
    scala.sys.process.Process.apply(
      vampirePath.toString,
      Seq("--input_syntax", "tptp", "--proof", "off", "--avatar", "off", "--time_limit", timeout.toString)).#<(new ByteArrayInputStream(problem.pretty.getBytes))
  }

  private def run_mace4(mace4Path: Path, problem: TPTP.Problem): process.ProcessBuilder = {
    // TODO: Ugly quick-shot implementation, just for testing. needs to be refactored.
    scala.sys.process.Process.apply(
      mace4Path.toString,
      Seq("-tptp", "-t", modelFinderTimeout.toString)).#<(new ByteArrayInputStream(problem.pretty.getBytes))
  }

}
object ProofSystemRunner {

  private final case class RunningProver(process: RunningProcess,
                                         stdout: StringBuffer,
                                         stderr: StringBuffer)

  def apply(premises: Seq[TPTP.AnnotatedFormula],
            conjecture: TPTP.AnnotatedFormula,
            system: ProofSystem,
            timeout: Int)
           (implicit externalSystemEc: ExecutionContext)
  = new ProofSystemRunner(premises, conjecture, system, timeout)(externalSystemEc)
}