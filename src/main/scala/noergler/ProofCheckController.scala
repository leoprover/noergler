package noergler

import leo.datastructures.TPTP
import leo.datastructures.TPTP.FOF
import noergler.ProofCheckController._
import noergler.checks.GenericInferenceCheck.{GenericInferenceCheckConfig, logger}
import noergler.checks._

import java.nio.file.Path
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Callable, Executors, RejectedExecutionException, TimeUnit}
import java.util.logging.Logger
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}

/**
 * The Controller coordinates the checking process, i.e., how and when
 * which checks are started etc (in parallel oder sequential).
 *
 * @param problem The source problem
 * @param proof The proof to be checked
 * @param configuration The checking configuration
 */
class ProofCheckController(proof: TPTP.Problem,
                           problem: Option[TPTP.Problem],
                           configuration: Configuration) {

  final val logger: Logger = Logger.getLogger("Nörgler.Controller")

  private final class QuietShutdownEC(underlying: ExecutionContextExecutorService,
                                      isShuttingDown: () => Boolean
                                     ) extends ExecutionContextExecutorService {

    override def execute(runnable: Runnable): Unit = {
      try {
        underlying.execute(runnable)
      } catch {
        case _: RejectedExecutionException if isShuttingDown() =>
          ()
      }
    }

    override def reportFailure(cause: Throwable): Unit = {
      cause match {
        case _: RejectedExecutionException if isShuttingDown() =>
          () // expected during shutdown; silence

        case other =>
          underlying.reportFailure(other)
      }
    }

    override def shutdown(): Unit = underlying.shutdown()
    override def shutdownNow(): util.List[Runnable] = underlying.shutdownNow()
    override def isShutdown: Boolean = underlying.isShutdown
    override def isTerminated: Boolean = underlying.isTerminated
    override def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = underlying.awaitTermination(timeout, unit)
    override def submit[T](task: Callable[T]): util.concurrent.Future[T] = underlying.submit[T](task: Callable[T])
    override def submit[T](task: Runnable, result: T): util.concurrent.Future[T] = underlying.submit[T](task: Runnable, result: T)
    override def submit(task: Runnable): util.concurrent.Future[_] = underlying.submit(task: Runnable)
    override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]]): util.List[util.concurrent.Future[T]] = underlying.invokeAll[T](tasks: util.Collection[_ <: Callable[T]])
    override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): util.List[util.concurrent.Future[T]] = underlying.invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit)
    override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]]): T = underlying.invokeAny[T](tasks: util.Collection[_ <: Callable[T]])
    override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): T = underlying.invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit)
  }

  private val shuttingDownECs = new AtomicBoolean(false)

  final private def threadCount: Int = Runtime.getRuntime.availableProcessors()*2

  private val orchestrationEC0: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(
      Executors.newFixedThreadPool(threadCount)
    )

  private val orchestrationEC: ExecutionContextExecutorService =
    new QuietShutdownEC(orchestrationEC0, () => shuttingDownECs.get())

  private implicit val ec: ExecutionContext = orchestrationEC

  private val externalSystemEc0: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(
      Executors.newFixedThreadPool(threadCount * 4)
    )

  private val externalSystemEc: ExecutionContextExecutorService =
    new QuietShutdownEC(externalSystemEc0, () => shuttingDownECs.get())

  Thread.setDefaultUncaughtExceptionHandler {
    case (thread, _: InterruptedException) if shuttingDownECs.get() &&
        thread.getName.startsWith("ThreadProcess-spawn-Thread") => ()

    case (_, ex) =>
      ex.printStackTrace()
  }

  /** Map of problem file TPTP annotated formula name (hopefully unique) -> the formula */
  private var problemFormulas: Map[String, TPTP.AnnotatedFormula] = Map.empty
  /** Name of the conjecture from the problem file, if known  */
  private var problemConjectureName: Option[String] = None

  /** The sequence of proof steps as they appear in the proof. */
  private val proofSteps: Seq[TPTP.AnnotatedFormula] = Vector.from(proof.formulas)
  /** Map of proof file TPTP annotated formula name (hopefully unique) -> the formula */
  private var proofFormulas: Map[String, TPTP.AnnotatedFormula] = Map.empty

  private var declarations: Seq[TPTP.AnnotatedFormula] = Seq.empty

  private var usedSkolemSymbols: Set[String] = Set.empty
  private lazy val problemSymbols: Set[String] =
    if (problem.isDefined) problem.get.formulas.flatMap(_.symbols).toSet
    else {
      // No problem file given -> reconstruct based on the problem formulas given in proof
      // todo: instead use the forumulas with annotation "file" here
      val problemFormulaRoles = Seq("axiom", "hypothesis", "definition", "assumption", "lemma", "theorem", "corollary", "conjecture", "negated_conjecture")
      val problemFormulas = proof.formulas.filter(f => problemFormulaRoles.contains(f.role))
      problemFormulas.flatMap(_.symbols).toSet
    }

  private final case class FutureHandle(result: Future[Unit], kill: () => Unit)

  private val killSignalAlreadySent = new AtomicBoolean(false)

  private var openFutures: Seq[FutureHandle] = Seq.empty

  private val already_showed_false: java.util.concurrent.atomic.AtomicReference[Option[Throwable]] = new java.util.concurrent.atomic.AtomicReference(None)
  private val already_showed_not_verifiable: java.util.concurrent.atomic.AtomicReference[Option[Throwable]] = new java.util.concurrent.atomic.AtomicReference(None)

  private def setFailedException(ex: VerificationFailedException): Unit = {
    already_showed_false.compareAndSet(None, Some(ex))

    if (killSignalAlreadySent.compareAndSet(false, true)) {
      logger.fine(s"Stopping all running proof checks because: ${ex.getMessage}")
      openFutures.foreach(_.kill())
    }
  }

  private def setNotVerifiableException(ex: VerificationTimedOutException): Unit = {
    if (killSignalAlreadySent.compareAndSet(false, true)) {

      already_showed_not_verifiable.compareAndSet(None, Some(ex))

      // todo change policy here to add tryHard2Fail mode

      logger.fine(s"Stopping all running proof checks because: ${ex.getMessage}")
      openFutures.foreach(_.kill())
    }
  }

  private def stoppingException: java.util.concurrent.atomic.AtomicReference[Option[Throwable]] =
    if (already_showed_false.get().isDefined) already_showed_false
    else already_showed_not_verifiable
  //todo: tryHard2fail mode that only accepts shown_false as stopping Exception and continues checking after a not_verifiable has been found

  def endAll(): Unit = {
    openFutures.foreach(_.kill)
  }

  final def apply(): Result = {
    //////////////////////////////////////////////////////////
    // preliminary steps
    //////////////////////////////////////////////////////////
    logger.config(s"Used configuration: ${configuration.toString}")
    if (problem.isDefined) {
      logger.fine("Processing problem file ...")
      // process problem file, read to map, initialize conjecture name
      for (af <- problem.get.formulas) {
        if (af.role == "type") declarations = declarations :+ af
        else {
          problemFormulas += (af.name -> af)
          if (af.role == "conjecture") problemConjectureName = Some(af.name)
        }
      }
      logger.info(s"Conjecture in problem found: ${problemConjectureName.toString}, ${problemFormulas.size - 1} axioms, ${declarations.size} type declarations.")
    }
    // process proof file, read to map, initialize conjecture name
    for (af <- proof.formulas) {
      proofFormulas += (af.name -> af)
    }

    //////////////////////////////////////////////////////////
    // Actual checking
    //////////////////////////////////////////////////////////
    logger.fine("Processing proof ...")
    try {
      //////////////////
      // specific checks
      //////////////////
      // (I.1) does the proof end with false?
      checkProofEndsInFalse()
      // (I.2) are the annotated formulas names in the proof unique?
      checkFormulaNamesAreUnique()
      // (I.3) are the inference parents acyclic?
      checkInferencesAreAcyclic()
      // ... more?

      //////////////////
      // iteration over every proof line, check each line depending on its character:
      //////////////////
      /** All the steps that have already been processed. Initially empty. */
      var previousProofSteps: Seq[TPTP.AnnotatedFormula] = Vector.empty
      for (proofstep <- proofSteps if !killSignalAlreadySent.get()) {
        var addedNewFuture = false
        var skippedStep = false
        logger.finer(s"Checking proof step '${proofstep.name}' with annotation '${proofstep.annotations.map(_._1.pretty).getOrElse("")}' ...")
        logger.finer(proofstep.annotations.toString)
        //////
        // II. Checks that every line has to do regardless of their specific annotation
        //////
        // (II.1) check that role of proof step is admissible
        checkRole(proofstep)
        // ... more?

        //////
        // III. Checks that every line has to do individually, specific to their annotation
        //////
        val annotation = proofstep.annotations
        annotationType(annotation) match {
          case Some(annotationType) => annotationType match {
            case "inference" =>
              // (III.0) check that every inference parent (if any) actually exists (earlier in the proof)
              checkInferenceParentsExist(proofstep, previousProofSteps)
              val inferenceStatus0 = inferenceStatus(annotation)

              inferenceName(annotation) match {
                case Some(inference) => inference match {
                  // (III.1) if a "negated_conjecture" entry, does it correctly negate and has correct role?
                  case "negated_conjecture" => checkNegatedInference(proofstep, inferenceStatus0)
                  case _ if configuration.relaxAnnotationFormat && proofstep.role == "negated_conjecture" && inferenceStatus0.contains(CTH) => checkNegatedInference(proofstep, inferenceStatus0)
                  // All cases that are not the negation of the conjecture
                  case non_conjecture_negation_cases =>
                    // (III.2) check that none of the inference parents (if any) are the conjecture
                    checkInferenceParentsAreNotConjecture(proofstep)
                    // (III.3) check that the inference status is not cth
                    checkStatusIsNotCth(proofstep, inferenceStatus0)
                    non_conjecture_negation_cases match {
                      // ESA cases
                      case rule if inferenceStatus0.contains(ESA) =>
                        if (configuration.upToESA) {
                          skippedStep = true
                          logger.info(s"Skipping verification of ESA step '${proofstep.name}'")
                        }
                        // (III.4) if a "skolemize" entry, does it correctly skolemize (use ASK)
                        else if (rule == "skolemize") checkSkolemization(proofstep, configuration.relaxAnnotationFormat)
                      // (III.5) if generic status(thm)/status(cth) entry, does it follow from its parents? (using external ATPs)
                      case rule if inferenceStatus0.contains(THM) =>
                        checkGenericInference(rule, proofstep, Left(THM), configuration.provers, configuration.modelFinder)
                        if (StepParallelisazionModes.contains(configuration.parallelMode)) addedNewFuture = true
                      case rule if inferenceStatus0.contains(CTH) =>
                        checkGenericInference(rule, proofstep, Right(CTH), configuration.provers, configuration.modelFinder)
                        if (StepParallelisazionModes.contains(configuration.parallelMode)) addedNewFuture = true
                      case _ => // Error case: unknown inference rule with non-THM/CTH status
                        logger.severe(s"Unknown inference '$inference' with status '${inferenceStatus0.getOrElse("")}' in proof step '${proofstep.name}'.")
                        throw new VerificationFailedException(s"Unknown inference rule '$inference' with non-thm/cth status ('${inferenceStatus0.getOrElse("")}'), cannot be checked.", Some(proofstep))
                    }
                }
                case None => // Unknown annotation, abort
                  logger.severe(s"Annotation of proof step '${proofstep.name}' unknown.")
                  throw new VerificationFailedException(s"Malformed inference annotation.", Some(proofstep))
              }
            case "file" => if (configuration.problemPath.isDefined) checkFormulaFromFile(proofstep, configuration.relaxProblemCheck)
            case "introduced" => ??? // TODO
            case record =>
              if (proofstep.role == "axiom" && configuration.allowProverAxioms){
                skippedStep = true
                logger.info(s"Prover introduced axiom ${proofstep.formula.pretty} (step '${proofstep.name}')")
              }
              else throw new VerificationFailedException(s"Unknown annotation record '$record'.", Some(proofstep))
          }
          case None if proofstep.role == "type" =>
            if (problem.isEmpty) declarations = declarations :+ proofstep
            logger.fine(s"found type annotation ${proofstep.pretty}")
          case None => // no annotation is an error for all steps.
            logger.severe(s"Proof step '${proofstep.name}' has no or malformed annotation.")
            throw new VerificationFailedException(s"No or malformed annotation.", Some(proofstep))
        }
        if (!skippedStep && (!StepParallelisazionModes.contains(configuration.parallelMode) || !addedNewFuture)) logger.info(s"Check succeeded for step '${proofstep.name}'.") // Assuming we fail fast if anything happens before
        previousProofSteps = previousProofSteps :+ proofstep
      }

      if (StepParallelisazionModes.contains(configuration.parallelMode)) {
        stoppingException.get() match {
          case Some(exception) =>
            throw exception
          case None =>
            logger.info(s"Waiting for verification tasks to finish ...")
            Await.result(Future.sequence(openFutures.map(_.result)), configuration.timeout.seconds)
            // TODO: Isnt this the same as before with extra steps? Discuss
            logger.info(s"All checks succeeded.")
        }
      }

      // report success
      logger.info("Proof verified.")
      Verified
    } catch {
      case e: VerificationFailedException =>
        if (e.proofstep.isDefined) logger.info(s"Check failed for step '${e.proofstep.get.name}'.")
        FailedVerified(e.getMessage)
      case e: VerificationTimedOutException =>
        NotVerified(e.getMessage)
      case _: concurrent.TimeoutException =>
        NotVerified("Timed out.")
    } finally {
      endAll()

      shuttingDownECs.set(true)
      orchestrationEC.shutdown()
      externalSystemEc.shutdown()
    }
  }

  //////////////////////////////////////////////////////////
  // Check delegate methods BEGIN
  //////////////////////////////////////////////////////////
  private def checkProofEndsInFalse(): Unit = {
    logger.fine("Check for $false at the end of proof.")
    val endsWithFalseCheck = ProofEndsInFalseCheck.apply(proof)
    logger.info(s"Proof ends in $$false: $endsWithFalseCheck")
    if (!endsWithFalseCheck) throw new VerificationFailedException("Proof does not end in false.")
  }

  private def checkFormulaNamesAreUnique(): Unit = {
    logger.fine("Check for uniqueness of formula names.")
    val namesUniqueCheck = FormulaNamesUniquenessCheck.apply(proofSteps)
    logger.info(s"Formula names unique: ${namesUniqueCheck.isEmpty}")
    if (namesUniqueCheck.isDefined) throw new VerificationFailedException(s"Annotated formula name '${namesUniqueCheck.get}' is used multiple times.")
  }

  private def checkInferencesAreAcyclic(): Unit = {
    logger.fine("Check for acyclicity of inference parents.")
    val inferenceParentsAreAcyclic = InferenceParentsAcyclicityCheck.apply(proofSteps, proofFormulas,configuration.relaxAnnotationFormat)
    logger.info(s"Inference parents are acyclic: $inferenceParentsAreAcyclic")
    if (!inferenceParentsAreAcyclic) throw new VerificationFailedException("Graph of inference parents from $false contains a cycle.")
  }

  private def checkInferenceParentsExist(proofstep: TPTP.AnnotatedFormula, previousProofSteps: Seq[TPTP.AnnotatedFormula]): Unit = {
    logger.finer("Check for existence of inference parents (if any).")
    val inferenceParentsCheck = InferenceParentsExistCheck.apply(proofstep, previousProofSteps, configuration.relaxAnnotationFormat)
    logger.fine(s"Inference parents exist (${proofstep.name}): $inferenceParentsCheck")
    if (!inferenceParentsCheck) throw new VerificationFailedException(s"(Some) inference parents unknown.", Some(proofstep))
  }

  private def checkInferenceParentsAreNotConjecture(proofstep: TPTP.AnnotatedFormula): Unit = {
    logger.finer("Check that the inference parents (if any) are not the conjecture.")
    val inferenceParentsNotConjCheck = InferenceParentsAreNotConjecture.apply(proofstep, problemConjectureName, configuration.relaxAnnotationFormat)
    logger.fine(s"Inference parents are not the conjecture (${proofstep.name}): $inferenceParentsNotConjCheck")
    if (!inferenceParentsNotConjCheck) throw new VerificationFailedException(s"Conjecture used as an inference parent.", Some(proofstep))
  }

  private def checkStatusIsNotCth(proofstep: TPTP.AnnotatedFormula, inferenceStatus0: Option[InferenceStatus]): Unit = {
    logger.finer("Check that the status of step (that is not the negation of the conjecture) is not cth.")
    val statusNotCthCheck = !inferenceStatus0.contains(CTH)
    logger.fine(s"Status of step (that is not the negation of the conjecture) is not cth (${proofstep.name}): $statusNotCthCheck")
    if (!statusNotCthCheck) throw new VerificationFailedException(s"It's not the negation of the conjecture but has status cth.", Some(proofstep))
  }

  private final val allowedRoles = Seq("axiom", "conjecture", "negated_conjecture", "plain", "type", "definition")
  private def checkRole(proofstep: TPTP.AnnotatedFormula): Unit = {
    if (problem.isEmpty && proofstep.role == "conjecture") {
      logger.finer(s"Found conjecture in proof: ${proofstep.name}")
      problemConjectureName = Some(proofstep.name)
    }
    val roleCheck = allowedRoles.contains(proofstep.role)
    if (!roleCheck) throw new VerificationFailedException(s"Unknown role.", Some(proofstep))
  }

  private def checkNegatedInference(proofstep: TPTP.AnnotatedFormula, inferenceStatus0: Option[InferenceStatus]): Unit = {
    if (inferenceStatus0.contains(CTH)){
      logger.finer("Check for correct negation of conjecture")
      val conjFormula =
        if (problem.isDefined) problemConjectureName.flatMap(problemFormulas.get)
        else problemConjectureName.flatMap(proofFormulas.get)
      val checkNegation = ConjectureNegationCheck.apply(proofstep, conjFormula)
      logger.fine(s"Negation of conjecture correct (${proofstep.name}): ${checkNegation._1}")
      if (!checkNegation._1) {
        if (configuration.relaxSpecifiedInferenceCheck && checkNegation._2.isDefined) {
          logger.info(s"Negation of the conjecture in the proof is not identical to internally derived negation. Fallback: Checking for entailment.")
          runFallbackEntailmentCheck(proofstep, checkNegation._2.get, Left(noergler.THM))
          //if (StepParallelisazionModes.contains(configuration.parallelMode)) addedNewFuture = true
          // todo: signal that new future was added
        }
        else throw new VerificationFailedException("Negation of conjecture is incorrect. Consider rerunning with flag --relax-specified-inference-check .", Some(proofstep))
      }
    }
    else throw new VerificationFailedException(s"Negation of the conjecture does not have the status cth (step ${proofstep.name}).")
  }

  private def checkSkolemization(proofstep: TPTP.AnnotatedFormula, relaxAnnotationFormat: Boolean): Unit = {
    logger.finer("Check for correct skolemization")
    val checkSkolemize = SkolemizationCheck.apply(proofstep, proofFormulas, usedSkolemSymbols ++ problemSymbols, relaxAnnotationFormat)
    checkSkolemize match {
      case Left(msg) =>
        throw new VerificationFailedException(s"Skolemization seems incorrect: $msg", Some(proofstep))
      case Right(skolemSymbolIntroduced) =>
        usedSkolemSymbols = usedSkolemSymbols + skolemSymbolIntroduced
    }
    logger.fine(s"Skolemization correct (${proofstep.name}): ${checkSkolemize.isRight}")
  }

  private def checkGenericInference(rule: String, proofstep: TPTP.AnnotatedFormula, status: Either[THM.type , CTH.type], provers: Set[Prover], modelFinder: ModelFinder, custumProofFormulas:  Option[Map[String, TPTP.AnnotatedFormula]] = None): Unit = {

    val usedProofFormulas = custumProofFormulas.getOrElse(proofFormulas)


    def run(): FutureHandle = {
      logger.finer(s"Check for correct entailment of inference rule '$rule'.")
      val individual_run_timeout = configuration.timeout // / 2 // todo: rasonable?
      assert(provers.nonEmpty)
      val inferenceCheckConfig = GenericInferenceCheckConfig(provers, modelFinder, ProverParallelisazionModes.contains(configuration.parallelMode), configuration.parallelCountermodelMode, configuration.relaxAnnotationFormat, individual_run_timeout)
      val stepHandle = GenericInferenceCheck(proofstep, usedProofFormulas, declarations, status, killSignalAlreadySent, inferenceCheckConfig,externalSystemEc)(orchestrationEC)

      val checkedFu0 = stepHandle.result.flatMap {
        case Some((true, usedProver)) =>
          logger.fine(s"${usedProver.name} found entailment result for ${proofstep.name}: true")
          Future.successful(())

        case Some((false, usedProver)) =>
          logger.fine(s"${usedProver.name} found entailment result for ${proofstep.name}: false")
          val exception = new VerificationFailedException(s"Inference ${proofstep.name} is provably incorrect.", Some(proofstep))
          setFailedException(exception)
          Future.failed(exception)

        // do not generate more failed futures if we already sent the kill signal
        case None if killSignalAlreadySent.get() =>
          Future.successful(())

        case None =>
          val exception = new VerificationTimedOutException(s"Verification of proof step '${proofstep.name}' timed out.")
          setNotVerifiableException(exception)
          Future.failed(exception)
      }

      // todo: how to best store the maps of running and planned processes so that I can easily kill them once a proof has been found?
      //  and should i even still have both the killing in the generic inference class and here in the controller?

      FutureHandle(checkedFu0,stepHandle.kill)
    }

    if (StepParallelisazionModes.contains(configuration.parallelMode)) {
      if (killSignalAlreadySent.get()) {
        logger.fine(s"Cancelling verification of step '${proofstep.name}' as proof has already shown to be false")
        return
      }
      val fu = run()
      if (killSignalAlreadySent.get()) {
        fu.kill
      } else {
        openFutures = openFutures :+ fu
      }
      logger.fine(s"Scheduled parallel inference check.")
    } else {
      val fu = run()
      Await.result(fu.result,configuration.timeout.seconds) // todo i need no further catch here, right?
    }

  }


  private def runFallbackEntailmentCheck(toBeProved: TPTP.AnnotatedFormula, premise: TPTP.FOF.Formula, status: Either[THM.type, CTH.type]): Unit = {

    // construct an annotated formula for the premise
    val namePremise = toBeProved.name + "_manually_created"
    val annotatedPremise: TPTP.FOFAnnotated = TPTP.FOFAnnotated(namePremise, "axiom", FOF.Logical(premise), None)

    // construct a map pointing to the annotated premise formula
    val custumProofFormulas: Map[String, TPTP.FOFAnnotated] = Map(namePremise -> annotatedPremise)

    // construct a version of the formula to be Proved that has (only) the given premise as a parent
    val annotations: TPTP.Annotations = constructInferenceAnnotation("fallback_entailment_check",Seq(namePremise))
    val annotatedToBeProved: TPTP.AnnotatedFormula = toBeProved.formula match {
      case f0@FOF.Logical(_) => TPTP.FOFAnnotated("c", "conjecture", f0, annotations)
      case _ => ??? //todo: other logics
    }


    // run generic inference check
    checkGenericInference("Fallback_entailment_check", annotatedToBeProved, status, configuration.provers, configuration.modelFinder, Some(custumProofFormulas))
  }

  private def checkFormulaFromFile(proofstep: TPTP.AnnotatedFormula, relaxProblemCheck: Boolean): Unit = {
    logger.finer("Check for correct premise usage from problem file.")
    assert(configuration.problemPath.isDefined)
    val proofPath = configuration.proofPath
    val problemPath = configuration.problemPath.get
    val checkFormulaFromFile = CorrectFormulaFromFileCheck.apply(proofstep, proofPath: Path, problemPath, problemFormulas, relaxProblemCheck)
    logger.fine(s"Formula equivalent to problem statement (${proofstep.name}): $checkFormulaFromFile")
    checkFormulaFromFile.foreach(err => throw new VerificationFailedException(err, Some(proofstep)))
  }
  //////////////////////////////////////////////////////////
  // Check delegate methods END
  //////////////////////////////////////////////////////////
}
object ProofCheckController {

  // Provers
  sealed trait ProofSystem {
    def name: String
    def path: Path
    def kind: String
  }

  sealed trait Prover extends ProofSystem {
    override final val kind = "prover"
  }

  sealed trait ModelFinder extends ProofSystem {
    override final val kind = "modelFinder"
  }

  case class EProver(path: Path) extends Prover { override final val name = "eprover" }
  case class Vampire(path: Path) extends Prover { override final val name = "vampire" }
  case class Mace4(path: Path) extends ModelFinder { override final val name = "mace4" }

  // Parallelization Modes
  sealed trait ParallelMode
  private case object Sequential extends ParallelMode
  private case object ParallelSteps extends ParallelMode
  private case object ParallelProvers extends ParallelMode
  private case object Hybrid extends ParallelMode

  sealed trait ParallelCountermodelMode
  case object NoModelFinder extends ParallelCountermodelMode
  case object Fallback extends ParallelCountermodelMode
  case class Offset(t: Int) extends ParallelCountermodelMode
  case object Always extends ParallelCountermodelMode

  private final val StepParallelisazionModes: Seq[ParallelMode] = Seq(ParallelSteps, Hybrid)
  private final val ProverParallelisazionModes: Seq[ParallelMode] = Seq(ParallelProvers, Hybrid)
  //val ModelCheckerParallelisazionModes = Seq(Offset(_), Always)

  private final case class Configuration(problemPath: Option[Path],
                                         proofPath: Path,
                                         timeout: Int,
                                         parallelMode: ParallelMode,
                                         parallelCountermodelMode: ParallelCountermodelMode,
                                         provers: Set[Prover],
                                         modelFinder: ModelFinder,
                                         ignoreFileAnnotations: Boolean,
                                         relaxAnnotationFormat: Boolean,
                                         relaxProblemCheck: Boolean,
                                         relaxSpecifiedInferenceCheck: Boolean,
                                         allowProverAxioms: Boolean,
                                         upToESA: Boolean)

  /** Thrown during the check if some step yields that the proof definitely cannot be verified
   * because it's not a valid proof. */
  private final class VerificationFailedException(msg: String, val proofstep: Option[TPTP.AnnotatedFormula] = None) extends RuntimeException(msg) {
    override def getMessage: String = {
      proofstep match {
        case Some(formula) => s"Verification failed for proof step: '${formula.name}'. ${super.getMessage}"
        case None => super.getMessage
      }
    }
  }

  /** Thrown during the check by some step if it gives up (e.g. timeout of external prover). */
  private class VerificationTimedOutException(msg: String) extends RuntimeException(msg)

  private final val defaultTimeout: Int = 60
  private final val defaultParallelMode: ParallelMode = Sequential
  private final val defaultParallelCountermodelMode: ParallelCountermodelMode = NoModelFinder
  private final val defaultIgnoreFileAnnotations: Boolean = false
  private final val defaultRelaxAnnotationFormat: Boolean = false
  private final val defaultRelaxProblemCheck: Boolean = false
  private final val defaultRelaxSpecifiedInferenceCheck: Boolean = false
  private final val defaultAllowProverAxioms: Boolean = false
  private final val defaultUpToESA: Boolean = false
  private final val defaultProvers: Set[String] = Set("eprover")
  private final val defaultModelFinder: String = "mace4"
  private final val defaulteproverPath: Option[String] = runSimpleCommand("which eprover")._1.headOption
  private final val defaultvampirePath: Option[String] = runSimpleCommand("which vampire")._1.headOption
  private final val defaultMace4Path: Option[String] = runSimpleCommand("which mace4")._1.headOption

  sealed abstract class Parameter
  final case class Timeout(timeout: Int) extends Parameter
  final case class SetParallelMode(mode: ParallelMode) extends Parameter
  case object SetParallelMode {
    final def parseArg(arg: String): SetParallelMode = {
      val mode = arg match {
        case "none" => Sequential
        case "steps" => ParallelSteps
        case "provers" => ParallelProvers
        case "hybrid" => Hybrid
        case _ => throw new IllegalArgumentException(s"Invalid parallel-mode '$arg'. Must be one of: 'none', 'steps', 'provers', 'hybrid'.")
      }
      SetParallelMode(mode)
    }
  }

  final case class SetParallelCountermodelMode(mode: String) extends Parameter
  object SetParallelCountermodelMode {
    final def parseArg(arg: String): SetParallelCountermodelMode = {
      val validModes = Set("none", "fallback", "always", "offset")
      if (!validModes.contains(arg)) {
        throw new IllegalArgumentException(s"Invalid parallel-countermodel-mode '$arg'. Must be one of: ${validModes.mkString(", ")}")
      }
      SetParallelCountermodelMode(arg)
    }
  }

  private case object IgnoreFileAnnotations extends Parameter //FIXME: Never parsed from CLI arguments
  case object RelaxAnnotationFormat extends Parameter
  case object RelaxProblemCheck extends Parameter
  case object RelaxSpecifiedInferenceCheck extends Parameter
  case object AllowProverAxioms extends Parameter
  case object UpToESA extends Parameter

  final case class ProverSelection(provers: Seq[String]) extends Parameter
  object ProverSelection {
    final def parseArg(arg: String): ProverSelection = {
      val selectedProvers = if (arg == "all") {
        List("eprover", "vampire")
      } else {
        arg.split(",").map(_.trim).toList
      }
      ProverSelection(selectedProvers)
    }
  }
  final case class ModelFinderSelection(modelFinder: String) extends Parameter
  final case class EproverPath(path: Path) extends Parameter
  final case class VampirePath(path: Path) extends Parameter
  final case class Mace4Path(path: Path) extends Parameter

  /** Factory method for a [[ProofCheckController]] based on the given arguments. */
  final def apply(problemPath: Option[Path],
                  proofPath:  Path,
                  problem: Option[TPTP.Problem],
                  proof: TPTP.Problem,
                  parameters: Seq[ProofCheckController.Parameter]): Result = {
    var timeout = defaultTimeout
    var parallel = defaultParallelMode
    var parallelCountermodel = defaultParallelCountermodelMode
    var selectedProvers = defaultProvers
    var selectedModelFinders = defaultModelFinder
    var ignoreFileAnnotations = defaultIgnoreFileAnnotations
    var relaxAnnotationFormat = defaultRelaxAnnotationFormat
    var relaxProblemCheck = defaultRelaxProblemCheck
    var relaxSpecifiedInferenceCheck = defaultRelaxSpecifiedInferenceCheck
    var allowProverAxioms = defaultAllowProverAxioms
    var upToESA = defaultUpToESA
    var eproverPath: Option[Path] = defaulteproverPath.map(p => Path.of(p))
    var vampirePath: Option[Path] = defaultvampirePath.map(p => Path.of(p))
    var mace4Path: Option[Path] = defaultMace4Path.map(p => Path.of(p))

    var useModelFinders: Boolean = false

    for (parameter <- parameters) {
      parameter match {
        case Timeout(to) => timeout = to
        case SetParallelMode(mode) => parallel = mode
        case SetParallelCountermodelMode(mode0) => parallelCountermodel = mode0 match {
          case "none" => NoModelFinder
          case "fallback" => useModelFinders = true; Fallback
          case "offset" => useModelFinders = true; Offset(1) //todo: make choosing time possibly
          case "always" => useModelFinders = true; Always
          case _ => NoModelFinder //todo: print message
        }
        case ProverSelection(names) => selectedProvers = names.toSet
        case ModelFinderSelection(name) => useModelFinders = true; selectedModelFinders = name
        case IgnoreFileAnnotations => ignoreFileAnnotations = true
        case RelaxAnnotationFormat => relaxAnnotationFormat = true
        case RelaxProblemCheck => relaxProblemCheck = true
        case RelaxSpecifiedInferenceCheck => relaxSpecifiedInferenceCheck = true
        case AllowProverAxioms => allowProverAxioms = true
        case UpToESA => upToESA = true
        case EproverPath(p) => eproverPath = Some(p)
        case VampirePath(p) => vampirePath = Some(p)
        case Mace4Path(p) => mace4Path = Some(p)
      }
    }

    var provers: Set[Prover] = selectedProvers.map {
      case "eprover" =>
        if (eproverPath.isEmpty) throw new IllegalArgumentException("eprover path unknown")
        EProver(eproverPath.get)
      case "vampire" =>
        if (vampirePath.isEmpty) throw new IllegalArgumentException("vampire path unknown")
        Vampire(vampirePath.get)
      case p => throw new IllegalArgumentException(s"Unknown prover '$p' requested")
    }

    val modelFinders: ModelFinder = {
      selectedModelFinders match {
        case "mace4" =>
          if (mace4Path.isEmpty) throw new IllegalArgumentException("mace4 path unknown")
          Mace4(mace4Path.get)
        case p => throw new IllegalArgumentException(s"Unknown model-finder '$p' requested")
      }
    }

    assert(provers.nonEmpty) //FIXME: Should we really use an assert here? maybe an if/else with exception?
    // check if parallelisazion mode requires mutliple provers and if multiple provers were chosen
    if (ProverParallelisazionModes.contains(parallel) && provers.size == 1){
      throw new IllegalArgumentException(s"Selected parallelisazion mode $parallel requires multiple provers, but only ${provers.head.name} was chosen. Either provide multiple provers explicitly, or set '--prover all'")
    }
    if (provers.size > 1 && parallel == Sequential) {
      parallelCountermodel match {
        case Offset(_) | Always =>
          logger.info(s"Sequential use of multiple provers in combination with model-checker parallelization is currently not supported. Only using ${provers.head.name} instead of ${provers.map(_.name).mkString(", ")}")
          provers = Set(provers.head)
        case _ => // nothing to do
      }
    }

    logger.info(s"prover size: ${provers.size}, parallel mode: $parallel, parallel countermodel: $parallelCountermodel")

    val config = Configuration(problemPath, proofPath, timeout, parallel, parallelCountermodel, provers, modelFinders, ignoreFileAnnotations, relaxAnnotationFormat, relaxProblemCheck, relaxSpecifiedInferenceCheck, allowProverAxioms, upToESA)
    val controller = new ProofCheckController(proof, problem, config)
    controller.apply()
  }
}
