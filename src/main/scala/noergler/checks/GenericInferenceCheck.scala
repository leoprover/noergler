package noergler.checks

import leo.datastructures.TPTP
import noergler.ProofCheckController.{ModelFinder, ProofSystem, Prover}
import noergler.checks.GenericInferenceCheck._
import noergler.{CTH, ProofCheckController, THM, proofStepParents}

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import java.util.logging.Logger
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.sys.process.{Process => RunningProcess}
import scala.util.{Failure, Success}

final class GenericInferenceCheck(premises: Seq[TPTP.AnnotatedFormula],
                                  conjecture: TPTP.AnnotatedFormula,
                                  externalKillSignal: AtomicBoolean,
                                  config: GenericInferenceCheckConfig,
                                  externalSystemEc: ExecutionContext)
                                 (implicit orchestrationEc: ExecutionContext)
{

  val running = TrieMap.empty[ProofSystem, (RunningProcess, Future[ProofSystemRunner.SZSRes])]
  val scheduled = TrieMap.empty[ProofSystem, ScheduledFuture[_]]
  val scheduler = Executors.newSingleThreadScheduledExecutor()

  private val ended = new AtomicBoolean(false)
  def endAll(): Unit = {
    if (ended.compareAndSet(false, true)) {
      cancelPlannedProcesses(scheduled)
      destroyRunningProcesses(running)
      scheduler.shutdown()
    }
  }
  val winner = Promise[Option[InferenceCheckResult]]()
  winner.future.onComplete { _ =>
    endAll()
  }

  /**
   * Run single prover, update the vars in case of definate result, keep track of running systems
   *
   * @param system
   * @return
   */
  def startSingleSystem0(system: ProofSystem): Future[ProofSystemRunner.SZSRes] = {
    val (newProc, fut) = ProofSystemRunner(premises, conjecture, system, config.individualStepTimeout)(externalSystemEc).apply()
    running.put(system, (newProc, fut))

    fut.onComplete { tr =>
      running.remove(system)
      tr.toOption.flatMap(res => toDefiniteResult(system, res)).foreach { res =>
        // system returned a definite result => set the system as the winner
        winner.trySuccess(Some(res))
      }
    }
    fut
  }

  def tagFutureWithSystem(system: ProofSystem, fu: Future[ProofSystemRunner.SZSRes]): Future[Option[InferenceCheckResult]] = {
    fu.map(res => Some(toInferenceResult(system, res)))
  }

  def startSingleSystem(system: ProofSystem): Future[Option[InferenceCheckResult]] = {
    if (winner.isCompleted) {
      logger.fine(s"Cancelling run of ${system.name} as step has already been verified/ shown to be false")
      Future.successful(None)
    }else if (externalKillSignal.get()) {
      logger.fine(s"Cancelling run of ${system.name} as proof has shown to be false")
      Future.successful(None)
    } else {
      val fu = startSingleSystem0(system)

      tagFutureWithSystem(system,fu)
    }
  }

  def startSerialSystems(systems: Seq[ProofSystem], inconclusiveResults: Vector[InferenceCheckResult] = Vector.empty): Future[Option[InferenceCheckResult]] = {
    if (winner.isCompleted) {
      logger.fine(s"Step already verified/ shown false. Cancelling serial run of provers.")
      Future.successful(None) //return
    } else if (externalKillSignal.get()) {
      logger.fine(s"Cancelling run of serial systems, as proof has shown to be false")
      Future.successful(None)
    } else{
      systems match {
        case Nil =>
          // no more provers to run => step cannot be verified, all systems timed out or gave up
          Future.successful(aggregateResults(inconclusiveResults.map(Some(_))))

        case system :: rest =>
          // run the system
          val fut = startSingleSystem(system)
          fut.flatMap {
            case Some(res) if isDefinite(res) =>
              Future.successful(Some(res))
            case Some(res) =>
              startSerialSystems(rest, inconclusiveResults :+ res)
            case None =>
              startSerialSystems(rest, inconclusiveResults)
          }
      }
    }
  }

  def runProversParallel(systems: Set[Prover]): Future[Option[InferenceCheckResult]] = {
    val futures = systems.map(startSingleSystem)
    Future.sequence(futures).map(aggregateResults)
  }

  private def destroyRunningProcesses(running: TrieMap[ProofSystem, (RunningProcess, Future[ProofSystemRunner.SZSRes])], keep: Option[ProofSystem] = None): Unit = {
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

  private def cancelPlannedProcesses(scheduled: TrieMap[ProofSystem, ScheduledFuture[_]], keep: Option[ProofSystem] = None): Unit = {
    scheduled.foreach {
      case (system, fu) if !keep.contains(system) =>
        logger.fine(s"Canceling ${system.kind} ${system.name}")
        try fu.cancel(true)
        catch {
          case ex: Throwable =>
            logger.fine(s"Failed to cancel ${system.name}: ${ex.getMessage}")
        }

      case _ => ()
    }
  }

  def apply(): Future[Option[InferenceCheckResult]] = {


    ///////////////////////////////////////////////////////
    // 1. Start the provers
    val proverFuture: Future[Option[InferenceCheckResult]] =
    if (config.provers.size == 1) {
      startSingleSystem(config.provers.head)

    } else if (config.parallelProvers){
      // start all selected provers in parallel
      runProversParallel(config.provers)

    } else {
      // run all provers in sequence
      startSerialSystems(config.provers.toSeq)
    }

    ///////////////////////////////////////////////////////
    // 2. Start the model finders
    val combinedFuture: Future[Option[InferenceCheckResult]] = config.parallelCountermodelMode match {
      case ProofCheckController.NoModelFinder =>
        proverFuture

      case ProofCheckController.Fallback =>
        // start model finder after all the provers have run and only if none of them gave a conclusive result
        proverFuture.flatMap{
          case fu@Some(res) if isDefinite(res) =>
            Future.successful(fu)

          case proverRes =>
            startSingleSystem(config.modelFinder).map { modelFinderRes =>
              aggregateResults(Seq(proverRes, modelFinderRes))
            }
        }

      case ProofCheckController.Offset(of_t) =>
        // If there is no result from the provers after x seconds, also start the counter model finder

        val mfStarted = new AtomicBoolean(false)
        val mfRunPromise = Promise[Future[Option[InferenceCheckResult]]]()

        def startModelFinder(): Unit = {
          if (mfStarted.compareAndSet(false, true)) {
            mfRunPromise.trySuccess(startSingleSystem(config.modelFinder))
          }
        }

        // schedule the model finder run after given offset
        val scheduledFuture = {
          // start model finder after given offset if none of the provers found a result
          val startModelChecker = new Runnable {
            override def run(): Unit = {
              if (!winner.isCompleted && !externalKillSignal.get()) {
                logger.info(s"No prover finished after ${of_t} seconds, starting model finder")
                startModelFinder()
              } else {
                mfRunPromise.trySuccess(Future.successful(None))
              }
            }
          }
          scheduler.schedule(startModelChecker, of_t.toLong, TimeUnit.SECONDS)
        }
        scheduled.put(config.modelFinder, scheduledFuture)

        // in case all provers finish before timeout, start MF earlier
        proverFuture.flatMap {
          case fu@Some(res) if isDefinite(res) =>
            scheduled.remove(config.modelFinder).foreach(_.cancel(false))
            Future.successful(fu)

          case proverRes =>
            scheduled.remove(config.modelFinder).foreach(_.cancel(false))
            startModelFinder()
            mfRunPromise.future.flatten.map { modelFinderRes =>
              aggregateResults(Seq(proverRes, modelFinderRes))
            }
        }

      case ProofCheckController.Always =>
        // at the same time start running the provers in sequence and in parallel to that also start the model finder in parallel
        val mfFuture = startSingleSystem(config.modelFinder)
        Future.sequence(Seq(proverFuture,mfFuture)).map(aggregateResults)
    }

    ///////////////////////////////////////////////////////
    // 3. Return a result

    combinedFuture.onComplete {
      case Success(res) =>
        winner.trySuccess(res)

      case Failure(ex) =>
        logger.warning(s"Parallel run of proof systems failed: ${ex.getMessage}")
        winner.trySuccess(None)
    }

    winner.future
  }
}

object GenericInferenceCheck {

  type RunningSystemMap = TrieMap[ProofSystem, (RunningProcess, Future[ProofSystemRunner.SZSRes])]
  type ScheduledSystemMap = TrieMap[ProofSystem, ScheduledFuture[_]]

  final val logger: Logger = Logger.getLogger("Nörgler.Controller")

  sealed trait InferenceCheckResult
  final case class EntailmentFound(system: ProofSystem) extends InferenceCheckResult
  final case class CountermodelFound(system: ProofSystem) extends InferenceCheckResult
  final case class InferenceTimedOut(systems: Seq[String]) extends InferenceCheckResult
  final case class InferenceUnknown(reasons: Seq[String]) extends InferenceCheckResult

  private final def isDefinite(result: InferenceCheckResult): Boolean = result match {
    case EntailmentFound(_) | CountermodelFound(_) => true
    case InferenceTimedOut(_) | InferenceUnknown(_) => false
  }

  private final def toInferenceResult(system: ProofSystem, result: ProofSystemRunner.SZSRes): InferenceCheckResult = {
    result match {
      case ProofSystemRunner.VerificationSuccess => EntailmentFound(system)
      case ProofSystemRunner.VerificationFail => CountermodelFound(system)
      case ProofSystemRunner.VerificationTimeout => InferenceTimedOut(Seq(system.name))
      case ProofSystemRunner.VerificationUnknown(status) => InferenceUnknown(Seq(s"${system.name}: $status"))
    }
  }

  private final def toDefiniteResult(system: ProofSystem, result: ProofSystemRunner.SZSRes): Option[InferenceCheckResult] = {
    toInferenceResult(system, result) match {
      case res if isDefinite(res) => Some(res)
      case _ => None
    }
  }

  private final def aggregateResults(results: Iterable[Option[InferenceCheckResult]]): Option[InferenceCheckResult] = {
    val observed = results.flatten.toSeq
    observed.collectFirst {
      case res if isDefinite(res) => res
    }.orElse {
      val timedOutSystems = observed.collect { case InferenceTimedOut(systems) => systems }.flatten.distinct
      val unknownReasons = observed.collect { case InferenceUnknown(reasons) => reasons }.flatten.distinct
      if (timedOutSystems.nonEmpty && unknownReasons.isEmpty) Some(InferenceTimedOut(timedOutSystems))
      else if (timedOutSystems.isEmpty && unknownReasons.nonEmpty) Some(InferenceUnknown(unknownReasons))
      else if (timedOutSystems.nonEmpty || unknownReasons.nonEmpty) Some(InferenceUnknown(Seq(s"none of the selected systems returned a conclusive SZS status")))
      else None
    }
  }

  final case class StepHandle (result: Future[Option[InferenceCheckResult]],
                               kill: () => Unit)

  case class GenericInferenceCheckConfig(provers: Set[Prover],
                                         modelFinder: ModelFinder,
                                         parallelProvers: Boolean,
                                         parallelCountermodelMode: ProofCheckController.ParallelCountermodelMode,
                                         relaxAnnotationFormat: Boolean,
                                         individualStepTimeout: Int)

  private final def constructInferenceProblem(proofstep: TPTP.AnnotatedFormula,
                                              names: Seq[String],
                                              proofFormulas: Map[String, TPTP.AnnotatedFormula],
                                              declarations: Seq[TPTP.AnnotatedFormula],
                                              status: Either[THM.type, CTH.type]): (Seq[TPTP.AnnotatedFormula], TPTP.AnnotatedFormula) = {
    import TPTP._
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

  final def apply(proofstep: TPTP.AnnotatedFormula,
                  proofFormulas: Map[String, TPTP.AnnotatedFormula],
                  declarations: Seq[TPTP.AnnotatedFormula],
                  status: Either[THM.type, CTH.type],
                  externalKill: AtomicBoolean,
                  config: GenericInferenceCheckConfig,
                  externalSystemEc: ExecutionContext)
                 (implicit orchestrationEc: ExecutionContext)= {

    val inferenceParentsNames = proofStepParents(proofstep.annotations, config.relaxAnnotationFormat)
    inferenceParentsNames match {
      case Some(names) =>
        // construct the inference problem
        val (premises, annotatedToBeProved) = constructInferenceProblem(proofstep, names, proofFormulas, declarations, status)
        val check = new GenericInferenceCheck(premises,annotatedToBeProved,externalKill,config,externalSystemEc)(orchestrationEc)

        val fu = check.apply()

        StepHandle(fu, check.endAll)

      case None =>
        logger.severe(s"Entailment check impossible (${proofstep.name}), inference parents entry malformed.")
        StepHandle(Future.successful(Some(CountermodelFound(config.provers.head))),() => ())
    }
  }

}
