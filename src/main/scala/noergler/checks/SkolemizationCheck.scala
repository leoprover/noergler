package noergler.checks

import leo.datastructures.TPTP
import noergler.{metaFunctionDataToFOF, proofStepParentsAsFormulas}

import java.util.logging.Logger

object SkolemizationCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.checks.SkolemizationCheck")

  @inline private final def fail(msg: String): Either[String, String] = Left(msg)

  final def apply(proofstep: TPTP.AnnotatedFormula,
                  proofFormulas: Map[String, TPTP.AnnotatedFormula],
                  alreadyUsedSkolemSymbols: Set[String],
                  relaxAnnotationFormat: Boolean): Either[String, String] = {
    // read of skolem inference details according to format
    // read of skolemSymbol name N (check: new)
    // read of variable V that was skolemized
    val inferenceRecord = extractSkolemizeRecordInfos(proofstep.annotations)
    inferenceRecord match {
      case Some((status, newSymbol, bind)) =>
        // fail fast if that's not well-formed, has non-esa status, is missing information
        if (status == "esa") {
          if (!alreadyUsedSkolemSymbols.contains(newSymbol)) {
            // read of parent from proof step (we know it exists)
            val inferenceParent = proofStepParentsAsFormulas(proofstep, proofFormulas, relaxAnnotationFormat)
            inferenceParent match {
              case Some(Seq(parent)) =>
                // execute trusted skolemization via ask using name N and variable V
                logger.fine(s"Formula to Skolemize: ${parent.pretty}")
                val ask = new leo.modules.skolemizer.SingleFormulaSkolemizer(newSymbol,
                  skolemizeAll = false, variableToSkolemize = Some(bind._1), choiceTerms = false)
                val manuallySkolemizedFormula = ask.apply(parent)
                val skolemTermUsedByAsk = ask.fofSkolemTerms.get(bind._1)
                logger.fine(s"Skolemized from proof: ${proofstep.formula.pretty}")
                logger.fine(s"Manually Skolemized result: ${manuallySkolemizedFormula.formula.pretty}")
                logger.finer(s"Skolem term by ask: ${skolemTermUsedByAsk.map(_.pretty).getOrElse("")}")
                logger.finer(s"Bind information: ${bind._2.pretty}")
                skolemTermUsedByAsk match {
                  case Some(askBind) =>
                    if (askBind == bind._2)
                      Either.cond(
                        manuallySkolemizedFormula.formula == proofstep.formula,
                        newSymbol,
                        s"Skolemization result in proof step ${proofstep.name} wrong. It should be ${manuallySkolemizedFormula.pretty}")
                    else {
                      val error = s"Skolemization bind record incorrect in step '${proofstep.name}'."
                      logger.severe(error)
                      fail(error)
                    }
                  case None =>
                    val error = s"Skolemization bind record cannot be compared to internal bind in step '${proofstep.name}'."
                    logger.severe(error)
                    fail(error)
                }
              case _ =>
                val error = s"Skolemization parents not well-formed in step '${proofstep.name}'."
                logger.severe(error)
                fail(error)
            }
          } else {
            val error = s"Skolemization inference record in step '${proofstep.name}' uses symbol that was already in use ('$newSymbol')."
            logger.severe(error)
            fail(error)
          }
        } else {
          val error = s"Skolemization inference record in step '${proofstep.name}' uses wrong status ($status)."
          logger.severe(error)
          fail(error)
        }
      case None =>
        val error = s"Skolemization inference record not well-formed in proof step '${proofstep.name}'."
        logger.severe(error)
        fail(error)
    }
  }

  private type Status = String
  private type NewSymbol = String
  private type Bind = (String, TPTP.FOF.Term)
  private type SkolemizeRecord = (Status, NewSymbol, Bind)
  /** Extract skolemization annotation. Has the form:
   * `inference(skolemize,[status(esa),new_symbols(skolem,[sK0]),skolemize(Bride,sK0(Marriage))],[marriage])`.
   *
   * Return value `None` means not well-formed record --> error case. */
  private final def extractSkolemizeRecordInfos(annotation: TPTP.Annotations): Option[SkolemizeRecord] = {
    var malformed = false
    var status: Option[Status] = None
    var newsymbol: Option[NewSymbol] = None
    var bind: Option[Bind] = None

    annotation match {
      case Some(anno) => anno._1.data match {
        case Seq(TPTP.MetaFunctionData("inference", args)) if args.size == 3 =>
          val skolemizationDetailsPart = args.tail.head
          if (skolemizationDetailsPart.data.isEmpty) {
            skolemizationDetailsPart.list match {
              case Some(gts) =>
                gts.foreach { gt =>
                  gt.data match {
                    case Seq(TPTP.MetaFunctionData("status", args)) =>
                      args match {
                        case Seq(TPTP.GeneralTerm(Seq(TPTP.MetaFunctionData(s, Seq())), None)) =>
                          if (status.isDefined) malformed = true
                          else status = Some(s)
                        case _ => malformed = true
                      }
                    case Seq(TPTP.MetaFunctionData("new_symbols", args)) =>
                      args match {
                        case Seq(TPTP.GeneralTerm(Seq(TPTP.MetaFunctionData("skolem", Seq())), None),
                                 TPTP.GeneralTerm(Seq(), Some(Seq(TPTP.GeneralTerm(Seq(TPTP.MetaFunctionData(symb, Seq())), None))))) =>
                          if (newsymbol.isDefined) malformed = true
                          else newsymbol = Some(symb)
                        case _ => malformed = true
                      }
                    case Seq(TPTP.MetaFunctionData("skolemize", args)) =>
                      args match {
                        case Seq(TPTP.GeneralTerm(Seq(TPTP.MetaVariable(v)), None),
                                 TPTP.GeneralTerm(Seq(data), None)) =>
                          val dataAsFOF = metaFunctionDataToFOF(data)
                          if (bind.isDefined || dataAsFOF.isEmpty) malformed = true
                          else bind = Some((v, dataAsFOF.get))
                        case _ => malformed = true
                      }
                    case _ => malformed = true // error
                  }
                }
                if (malformed || Seq(status, newsymbol, bind).exists(_.isEmpty)) None
                else Some((status.get, newsymbol.get, bind.get))
              case None => None
            }
          } else None
        case _ => None
      }
      case None => None
    }
  }
}
