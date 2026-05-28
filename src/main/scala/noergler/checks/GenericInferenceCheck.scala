package noergler.checks

import leo.datastructures.TPTP
import noergler.ProofCheckController.{ModelFinder, ProofSystem, Prover}
import noergler.checks.GenericInferenceCheck.logger
import noergler.{CTH, ProofCheckController, THM, proofStepParents}

import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.logging.Logger
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.sys.process
import scala.sys.process.{ProcessLogger, Process => RunningProcess}
import java.util.concurrent.{Executors, TimeUnit}

final class GenericInferenceCheck(premises: Seq[TPTP.AnnotatedFormula],
                                  conjecture: TPTP.AnnotatedFormula,
                                  system: ProofSystem,
                                  //already_showed_false: AtomicReference[Option[Throwable]],
                                  timeout: Int)
                                 (implicit ec: ExecutionContext) {
  def apply_parallel(): (RunningProcess, Future[Option[Boolean]]) = ProofSystemRunner(premises, conjecture, system, timeout).apply()


  def apply_serial(): Option[Boolean] = {
    val (_, fut0) = apply_parallel()

    try {
      Await.result(fut0, timeout.seconds)
    } catch {
      case _: java.util.concurrent.TimeoutException => None

    }

  }
}

  object GenericInferenceCheck {
    final val logger: Logger = Logger.getLogger("Nörgler.Controller") //todo: should this live here or elsewhere?

    final case class SerialInferenceConfig(prover: ProofSystem,
                                           timeout: Int,
                                           relaxAnnotationFormat: Boolean,
                                           declarations: Seq[TPTP.AnnotatedFormula]
                                          )

    final case class ParallelInferenceConfig(provers: Set[Prover],
                                             modelFinder: Option[ModelFinder],
                                             offset: Int,
                                             timeout: Int,
                                             relaxAnnotationFormat: Boolean,
                                             declarations: Seq[TPTP.AnnotatedFormula]
                                            )

    private final def constructInferenceProblem(proofstep: TPTP.AnnotatedFormula,
                                                names: Seq[String],
                                                proofFormulas: Map[String, TPTP.AnnotatedFormula],
                                                declarations: Seq[TPTP.AnnotatedFormula],
                                                status: Either[THM.type, CTH.type]): (Seq[TPTP.AnnotatedFormula], TPTP.AnnotatedFormula) = {
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
      //todo: only pass the declarations that are actually necessary?
      (declarations ++ premises, annotatedToBeProved)
    }

    final def apply_serial(proofstep: TPTP.AnnotatedFormula,
                           proofFormulas: Map[String, TPTP.AnnotatedFormula],
                           status: Either[THM.type, CTH.type],
                           config: SerialInferenceConfig)
                          (implicit ec: ExecutionContext): Option[Boolean] = {
      val inferenceParentsNames: Option[Seq[String]] = proofStepParents(proofstep.annotations, config.relaxAnnotationFormat)
      inferenceParentsNames match {
        case Some(names) =>
          val (premises, annotatedToBeProved) = constructInferenceProblem(proofstep, names, proofFormulas, config.declarations, status)
          new GenericInferenceCheck(premises, annotatedToBeProved, config.prover, config.timeout).apply_serial()
        case None =>
          logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
          Some(false)
      }
    }


    private type RunningEntry = (RunningProcess, Future[Option[Boolean]])

    private def destroyRunningProcesses(running: TrieMap[ProofSystem, RunningEntry], keep: Option[ProofSystem] = None): Unit = {
      running.foreach {
        case (system, (proc, _)) if !keep.contains(system) =>
          logger.fine(s"Destroying ${system.kind} ${system.name}")
          try proc.destroy()
          catch {
            case ex: Throwable =>
              logger.fine(s"Failed to destroy ${system.name}: ${ex.getMessage}")
          }

        case _ => ()
      }
    }

    final def apply_parallel(proofstep: TPTP.AnnotatedFormula,
                             proofFormulas: Map[String, TPTP.AnnotatedFormula],
                             status: Either[THM.type, CTH.type],
                             already_showed_false: AtomicReference[Option[Throwable]],
                             config: ParallelInferenceConfig)
                            (implicit ec: ExecutionContext): (Option[Boolean], ProofSystem) = {

      val completed = new AtomicBoolean(false)
      val winner = Promise[(Option[Boolean], ProofSystem)]()
      val running = TrieMap.empty[ProofSystem, (RunningProcess, Future[Option[Boolean]])]
      // keep track of finished results to not wait for entire timeout if provers give up early
      val remaining = new java.util.concurrent.atomic.AtomicInteger(0)

      def startProcess(system: ProofSystem, premises: Seq[TPTP.AnnotatedFormula], annotatedToBeProved: TPTP.AnnotatedFormula): Unit = {
        if (already_showed_false.get().isDefined) {
          logger.fine(s"Cancelling verification of step '${proofstep.name}' as proof has already shown to be false")
          return
        }

        remaining.incrementAndGet()

        val checker = new GenericInferenceCheck(premises, annotatedToBeProved, system, config.timeout)(ec)
        val (proc, fut) = checker.apply_parallel()

        running.put(system, (proc, fut))

        fut.onComplete { tr =>
          val res = tr.toOption.flatten

          if (res.isDefined && completed.compareAndSet(false, true)) {
            winner.trySuccess((res, system))
          } else {
            val left = remaining.decrementAndGet()
            if (left == 0 && completed.compareAndSet(false, true)) {
              winner.trySuccess((None, config.provers.head))
            }
          }
        }(ec)
      }

      val inferenceParentsNames = proofStepParents(proofstep.annotations, config.relaxAnnotationFormat)
      inferenceParentsNames match {
        case Some(names) =>
          // construct the inference problem
          val (premises, annotatedToBeProved) = constructInferenceProblem(proofstep, names, proofFormulas, config.declarations, status)

          // start all provers
          config.provers.foreach { prover =>
            startProcess(prover, premises, annotatedToBeProved)
          }

          val scheduler = Executors.newSingleThreadScheduledExecutor()

          // start model finder after given offset if none of the provers found a result
          if (config.modelFinder.isDefined) {
            val startModelChecker = new Runnable {
              override def run(): Unit = {
                if (!completed.get() && already_showed_false.get().isEmpty) {
                  logger.info(s"No prover finished after ${config.offset} seconds, starting model finder")
                  startProcess(config.modelFinder.get, premises, annotatedToBeProved)
                }
              }
            }
            scheduler.schedule(startModelChecker, config.offset.toLong, TimeUnit.SECONDS)
          }

          try {
            val result = Await.result(winner.future, (config.timeout + 5).seconds)

            // kill all other processes
            logger.info(s"Destroying external systems for step ${proofstep.name}...")
            destroyRunningProcesses(running, Some(result._2))

            result
          } catch {
            case _: java.util.concurrent.TimeoutException =>
              // nobody produced Some(...) in time -> kill all
              running.foreach {
                case (_, (proc, _)) =>
                  try proc.destroy()
                  catch {
                    case ex: Throwable =>
                      logger.fine(s"Failed to destroy prover process: ${ex.getMessage}")
                  }
              }
              (None, config.provers.head)
          } finally {
            scheduler.shutdown()
          }

        case None =>
          logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
          (Some(false), config.provers.head)
      }
    }

}
