package noergler.checks

import leo.datastructures.TPTP
import leo.datastructures.TPTP.{CNF, FOF, TCF, TFF, THF}

object ProofEndsInFalseCheck {
  @inline private[this] final val falseFormula = "$false"
  final def apply(proof: TPTP.Problem): Boolean = {
    if (proof.formulas.nonEmpty) {
      val lastProofStep = proof.formulas.last
      lastProofStep match {
        case TPTP.FOFAnnotated(_, _, formula, _) => formula match {
          case FOF.Logical(formula1) => formula1 match {
            case FOF.AtomicFormula(`falseFormula`, Seq()) => true
            case _ => false
          }
        }
        case TPTP.THFAnnotated(_, _, formula, _) =>
          formula match {
            case THF.Logical(formula1) => formula1 match {
              case THF.FunctionTerm(`falseFormula`, Seq()) => true
              case _ => false
            }
            case _ => false
          }
        case TPTP.TFFAnnotated(_, _, formula, _) => formula match {
          case TFF.Logical(formula1) => formula1 match {
            case TFF.AtomicFormula(`falseFormula`, Seq()) => true
          }
          case _ => false
        }
        case TPTP.TCFAnnotated(_, _, formula, _) => formula match {
          case TCF.Logical(formula1) => formula1.clause.isEmpty
          case _ => false
        }
        case TPTP.CNFAnnotated(_, _, formula, _) => formula match {
          case CNF.Logical(formula1) => formula1.isEmpty
        }
        case _ => false
      }
    } else false
  }
}
