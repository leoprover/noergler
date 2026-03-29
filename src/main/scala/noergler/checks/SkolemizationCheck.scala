package noergler.checks

import leo.datastructures.TPTP
import noergler.proofStepParentsAsFormulas

import java.util.logging.Logger

class SkolemizationCheck {

}
object SkolemizationCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.checks.SkolemizationCheck")

  @inline private final def fail(msg: String): Either[String, String] = Left(msg)
//  @inline private final def succeed(skolemSymbol: String): Either[String, String] = Right(skolemSymbol)

  final def apply(proofstep: TPTP.FOFAnnotated,
                  proofFormulas: Map[String, TPTP.FOFAnnotated],
                  alreadyUsedSkolemSymbols: Set[String]): Either[String, String] = {
    logger.finer(s"proofstep annotation: ${proofstep.annotations.toString}")
    // read of skolem inference details according to format
    // read of skolemSymbol name N (check: new)
    // read of variable V that was skolemized
    val inferenceRecord = extractSkolemizeRecordInfos(proofstep.annotations)
    inferenceRecord match {
      case Some((status, newSymbol, variable, bind)) =>
        // fail fast if that's not well-formed, has non-esa status, is missing information
        if (status == "esa") {
          if (variable == bind._1) {
            if (!alreadyUsedSkolemSymbols.contains(newSymbol)) {
              // read of parent from proof step (we know it exists)
              val inferenceParent = proofStepParentsAsFormulas(proofstep, proofFormulas)
              inferenceParent match {
                case Some(Seq(parent)) =>
                  // execute trusted skolemization via ask using name N and variable V
                  logger.fine(s"Formula to Skolemize: ${parent.pretty}")
                  val ask = new leo.modules.skolemizer.SingleFormulaSkolemizer(newSymbol,
                    skolemizeAll = false, variableToSkolemize = Some(variable), choiceTerms = false)
                  val manuallySkolemizedFormula = ask.apply(parent)
                  logger.fine(s"Skolemized from proof: ${proofstep.formula.pretty}")
                  logger.fine(s"Manually Skolemized result: ${manuallySkolemizedFormula.pretty}")
                  // compare results
                  // TODO: Do we need to check if the bind information is correct?
                  Either.cond(
                    manuallySkolemizedFormula.formula == proofstep.formula,
                    newSymbol,
                    s"Skolemization result in proof step ${proofstep.name} wrong. It should be ${manuallySkolemizedFormula.pretty}")
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
            val error = s"Skolemization inference record in step '${proofstep.name}' uses wrong variable name in binding record (${bind._1} := ${bind._2.toString})"
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
  private type SkolemizedVariable = String
  private type Bind = (String, TPTP.MetaFunctionData)
  private type SkolemizeRecord = (Status, NewSymbol, SkolemizedVariable, Bind)
  /** None means not well-formed record --> error case. */
  private final def extractSkolemizeRecordInfos(annotation: TPTP.Annotations): Option[SkolemizeRecord] = {
    var malformed = false
    var status: Option[Status] = None
    var newsymbol: Option[NewSymbol] = None
    var skolemizedVariable: Option[SkolemizedVariable] = None
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
                    case Seq(TPTP.MetaFunctionData("skolemized", args)) =>
                      args match {
                        case Seq(TPTP.GeneralTerm(Seq(TPTP.MetaVariable(v)), None)) =>
                          if (skolemizedVariable.isDefined) malformed = true
                          else skolemizedVariable = Some(v)
                        case _ => malformed = true
                      }
                    case Seq(TPTP.MetaFunctionData("bind", args)) =>
                      args match {
                        case Seq(TPTP.GeneralTerm(Seq(TPTP.MetaVariable(v)), None),
                                 TPTP.GeneralTerm(Seq(mf@TPTP.MetaFunctionData(_, _)), None)) =>
                          if (bind.isDefined) malformed = true
                          else bind = Some((v, mf))
                        case _ => malformed = true
                      }
                    case _ => malformed = true // error
                  }
                }
                if (malformed) None
                else Some((status.get, newsymbol.get, skolemizedVariable.get, bind.get))
              case None => None
            }
          } else None
        case _ => None
      }
      case None => None
    }
  }
}
