package noergler.checks

import leo.datastructures.TPTP
import noergler.ProofCheckController.Prover
import noergler.checks.GenericInferenceCheck.logger
import noergler.{CTH, ProofCheckController, THM, proofStepParents}

import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.sys.process
import scala.sys.process.{ProcessLogger, Process => RunningProcess}

final class GenericInferenceCheck(premises: Seq[TPTP.AnnotatedFormula],
                                  conjecture: TPTP.AnnotatedFormula,
                                  prover: Prover,
                                  timeout: Int)
                                 (implicit ec: ExecutionContext) {

  private final case class RunningProver( process: RunningProcess,
                                          stdout: StringBuffer,
                                          stderr: StringBuffer
                                        )

  private def startProcess():RunningProver = {
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
    RunningProver(running, stdout, stderr)
  }


  private def collectResult(started: RunningProver): Option[Boolean] = {
    val exitCode = started.process.exitValue() // blocks until process exits
    val errResult = started.stderr.toString
    logger.finest(s"${prover.name} stderr: $errResult")
    val result = started.stdout.toString
    logger.finest(s"${prover.name} exit code: $exitCode")
    logger.finest(s"${prover.name} response: $result")
    parse_TSTP_output(result)
  }

  def apply_parallel(): (RunningProcess, Future[Option[Boolean]]) = {
    val started = startProcess()
    val resultFuture = Future {
      collectResult(started)
    }
    (started.process, resultFuture)
  }


  def apply_serial(): Option[Boolean] = {
    val started = startProcess()
    collectResult(started)
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
      Seq("--input_syntax","tptp","--proof","off","--avatar","off","--time_limit",timeout.toString)).#<(new ByteArrayInputStream(problem.pretty.getBytes))
  }

}
object GenericInferenceCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.Controller")

  final case class SerialInferenceConfig(prover: Prover,
                                         timeout: Int,
                                         relaxAnnotationFormat: Boolean
                                  )

  final case class ParallelInferenceConfig(provers: Set[Prover],
                                         timeout: Int,
                                         relaxAnnotationFormat: Boolean
                                        )

  final def constructInferenceProblem(proofstep: TPTP.AnnotatedFormula,
                                      names: Seq[String],
                                      proofFormulas: Map[String, TPTP.AnnotatedFormula],
                                      status: Either[THM.type , CTH.type]): (Seq[TPTP.AnnotatedFormula], TPTP.AnnotatedFormula) = {
    import TPTP.{THF, TFF, FOF, TCF, CNF}
    val inferenceParents = names.map(proofFormulas) // safe as we checked the existence of all parents before
    logger.finer(s"Inference parents: ${names.mkString(",")}")
    val premises: Seq[TPTP.AnnotatedFormula] = inferenceParents.map {
      case TPTP.THFAnnotated(name, _, formula, _) => TPTP.THFAnnotated(name, "axiom", formula, None)
      case TPTP.TFFAnnotated(name, _, formula, _) => TPTP.TFFAnnotated(name, "axiom", formula, None)
      case TPTP.FOFAnnotated(name, _, formula, _) => TPTP.FOFAnnotated(name, "axiom", formula, None)
      case TPTP.TCFAnnotated(name, _, formula, _) => TPTP.TCFAnnotated(name, "axiom", formula, None)
      case TPTP.CNFAnnotated(name, _, formula, _) => TPTP.CNFAnnotated(name, "axiom", formula, None)
      case TPTP.TPIAnnotated(name, _, formula, _) => TPTP.TPIAnnotated(name, "axiom", formula, None)
    }
    val formulatoBeProved: proofstep.F = status match {
      case Left(_) => proofstep.formula
      case Right(_) => proofstep.formula match {
        case THF.Logical(formula) => THF.Logical(THF.UnaryFormula(THF.~, formula)).asInstanceOf[proofstep.F]
        case TFF.Logical(formula) => TFF.Logical(TFF.UnaryFormula(TFF.~, formula)).asInstanceOf[proofstep.F]
        case FOF.Logical(formula) => FOF.Logical(FOF.UnaryFormula(FOF.~, formula)).asInstanceOf[proofstep.F]
        case TCF.Logical(_) => ??? // TODO
        case CNF.Logical(_) => ??? // TODO
        case _ => throw new IllegalArgumentException("TPI formulas cannot be used for verification.")
      }
    }
    val annotatedToBeProved: TPTP.AnnotatedFormula = proofstep match {
      case TPTP.THFAnnotated(name, _, _, _) =>
        TPTP.THFAnnotated(name, "conjecture", formulatoBeProved.asInstanceOf[TPTP.THF.Statement], None)
      case TPTP.TFFAnnotated(name, _, _, _) =>
        TPTP.TFFAnnotated(name, "conjecture", formulatoBeProved.asInstanceOf[TPTP.TFF.Statement], None)
      case TPTP.FOFAnnotated(name, _, _, _) =>
        TPTP.FOFAnnotated(name, "conjecture", formulatoBeProved.asInstanceOf[TPTP.FOF.Statement], None)
      case TPTP.TCFAnnotated(name, _, _, _) => // FIXME: I think TCF/CNF does not have conjectures
        TPTP.TCFAnnotated(name, "conjecture", formulatoBeProved.asInstanceOf[TPTP.TCF.Statement], None)
      case TPTP.CNFAnnotated(name, _, _, _) =>
        TPTP.CNFAnnotated(name, "conjecture", formulatoBeProved.asInstanceOf[TPTP.CNF.Statement], None)
      case _ => throw new IllegalArgumentException("TPI formulas cannot be used for verification.")
    }
    (premises, annotatedToBeProved)
  }

  final def apply_serial(proofstep: TPTP.AnnotatedFormula,
                  proofFormulas: Map[String, TPTP.AnnotatedFormula],
                  status: Either[THM.type , CTH.type],
                  config: SerialInferenceConfig)
                 (implicit ec: ExecutionContext): Option[Boolean] = {
    val inferenceParentsNames: Option[Seq[String]] = proofStepParents(proofstep.annotations, config.relaxAnnotationFormat)
    inferenceParentsNames match {
      case Some(names) =>
        val (premises, annotatedToBeProved) = constructInferenceProblem(proofstep,names, proofFormulas, status)
        new GenericInferenceCheck(premises, annotatedToBeProved, config.prover, config.timeout).apply_serial()
      case None =>
        logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
        Some(false)
    }
  }

  final def apply_parallel(proofstep: TPTP.AnnotatedFormula,
                         proofFormulas: Map[String, TPTP.AnnotatedFormula],
                         status: Either[THM.type, CTH.type],
                         config: ParallelInferenceConfig)
                        (implicit ec: ExecutionContext): (Option[Boolean], Prover) = {
    val inferenceParentsNames = proofStepParents(proofstep.annotations, config.relaxAnnotationFormat)
    inferenceParentsNames match {
      case Some(names) =>
        val (premises, annotatedToBeProved) = constructInferenceProblem(proofstep, names, proofFormulas, status)
        val completed = new AtomicBoolean(false)
        val winner = Promise[(Option[Boolean], Prover)]()

        // run all processes and update the result as soon as prover finishes
        val running = config.provers.toSeq.map { prover =>
          val checker = new GenericInferenceCheck(premises, annotatedToBeProved, prover, config.timeout)(ec)
          val (proc, fut) = checker.apply_parallel()

          fut.onComplete { tr =>
            val res = tr.toOption.flatten
            if (res.isDefined && completed.compareAndSet(false, true)) winner.trySuccess((res, prover))
          }(ec)
          (prover, proc, fut)
        }

        try {
          val result = Await.result(winner.future, (config.timeout + 5).seconds)

          // kill all other processes
          running.foreach {
            case (prover, proc, _) if prover != result._2 =>
              logger.info(s"Destroying losing prover ${prover.name} for step ${proofstep.name}")
              try proc.destroy()
              catch {
                case ex: Throwable =>
                  logger.fine(s"Failed to destroy ${prover.name}: ${ex.getMessage}")
              }

            case _ => ()
          }

          result
        } catch {
          case _: java.util.concurrent.TimeoutException =>
            // nobody produced Some(...) in time -> kill all
            running.foreach {
              case (_, proc, _) =>
                try proc.destroy()
                catch {
                  case ex: Throwable =>
                    logger.fine(s"Failed to destroy prover process: ${ex.getMessage}")
                }
            }
            (None, config.provers.head)
        }

      case None =>
        logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
        (Some(false), config.provers.head)
    }
  }
}
