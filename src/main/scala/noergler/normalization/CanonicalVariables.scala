package noergler.normalization

import leo.datastructures.TPTP
import leo.datastructures.TPTP.{FOF, TFF}

/**
 * For renaming (bound) variables in a formula/term in a canonical way.
 */
final class CanonicalVariables {
  import CanonicalVariables.variableBaseName
  private var lastNumber: Int = 0
  private def nextVariable(): String = {
    lastNumber = lastNumber + 1
    s"$variableBaseName$lastNumber"
  }

  def apply(formula: TPTP.AnnotatedFormula#F): TPTP.AnnotatedFormula#F = {
    formula match {
      case TPTP.THF.Logical(formula) => TPTP.THF.Logical(thfFormula(formula)).asInstanceOf[TPTP.AnnotatedFormula#F]
      case TPTP.TFF.Logical(formula) => TPTP.TFF.Logical(tffFormula(formula)).asInstanceOf[TPTP.AnnotatedFormula#F]
      case TPTP.FOF.Logical(formula) => TPTP.FOF.Logical(fofFormula(formula)).asInstanceOf[TPTP.AnnotatedFormula#F]
      case TPTP.TCF.Logical(formula) => TPTP.TCF.Logical(tcfFormula(formula)).asInstanceOf[TPTP.AnnotatedFormula#F]
      case TPTP.CNF.Logical(formula) => TPTP.CNF.Logical(cnfFormula(formula)).asInstanceOf[TPTP.AnnotatedFormula#F]
      case _ => throw new IllegalArgumentException("Only logic formulas supported in THF/TFF/FOF/TCF/CNF.")
    }
  }

  private def thfFormula(formula: TPTP.THF.Formula): TPTP.THF.Formula = {
    formula // TODO
  }

  private def tffFormula(formula: TPTP.TFF.Formula): TPTP.TFF.Formula = {
    tffFormula0(formula, Map.empty)
  }
  private def tffFormula0(formula: TPTP.TFF.Formula, subst: Map[String, String]): TPTP.TFF.Formula = {
    formula match {
      case TFF.AtomicFormula(f, args) =>
        TFF.AtomicFormula(f, args.map(tffTerm0(_, subst)))
      case TFF.QuantifiedFormula(quantifier, variableList, body) =>
        var map0 = subst
        var variableList0: Seq[TPTP.TFF.TypedVariable] = Seq.empty
        variableList.foreach { vari =>
          val newVar = nextVariable()
          map0 = map0 + (vari._1 -> newVar)
          variableList0 = variableList0 :+ (newVar, vari._2)
        }
        val body0 = tffFormula0(body, map0)
        TFF.QuantifiedFormula(quantifier, variableList0, body0)
      case TFF.UnaryFormula(connective, body) =>
        TFF.UnaryFormula(connective, tffFormula0(body, subst))
      case TFF.BinaryFormula(connective, left, right) =>
        val left0 = tffFormula0(left, subst)
        val right0 = tffFormula0(right, subst)
        TFF.BinaryFormula(connective, left0, right0)
      case TFF.Equality(left, right) =>
        val left0 = tffTerm0(left, subst)
        val right0 = tffTerm0(right, subst)
        TFF.Equality(left0, right0)
      case TFF.Inequality(left, right) =>
        val left0 = tffTerm0(left, subst)
        val right0 = tffTerm0(right, subst)
        TFF.Inequality(left0, right0)
      case TFF.FormulaVariable(name) =>
        subst.get(name) match {
          case Some(name0) => TFF.FormulaVariable(name0)
          case None => TFF.FormulaVariable(name) // Should not happen if formula/term is closed. Edge case
        }
      case TFF.ConditionalFormula(condition, thn, els) =>
        TFF.ConditionalFormula(tffFormula0(condition, subst),
          tffTerm0(thn, subst), tffTerm0(els, subst))
      case TFF.LetFormula(typing, binding, body) => TFF.LetFormula(typing, binding, body) // TODO
      case TFF.NonclassicalPolyaryFormula(connective, args) =>
        TFF.NonclassicalPolyaryFormula(connective, args.map(tffFormula0(_, subst)))
      case _ => formula
    }
  }
  private def tffTerm0(term: TPTP.TFF.Term, subst: Map[String, String]): TPTP.TFF.Term = term match {
    case TFF.AtomicTerm(f, args) => TFF.AtomicTerm(f, args.map(tffTerm0(_, subst)))
    case TFF.Variable(name) =>
      subst.get(name) match {
      case Some(name0) => TFF.Variable(name0)
      case None => TFF.Variable(name) // Should not happen if formula/term is closed. Edge case
    }
    case TFF.Tuple(elements) => TFF.Tuple(elements.map(tffTerm0(_, subst)))
    case TFF.FormulaTerm(formula) => TFF.FormulaTerm(tffFormula0(formula, subst))
    case _ => term
  }

  private def fofFormula(formula: TPTP.FOF.Formula): TPTP.FOF.Formula = {
    fofFormula0(formula, Map.empty)
  }
  private def fofFormula0(formula: TPTP.FOF.Formula, subst: Map[String, String]): TPTP.FOF.Formula = {
    formula match {
      case FOF.AtomicFormula(f, args) =>
        FOF.AtomicFormula(f, args.map(fofTerm0(_, subst)))
      case FOF.QuantifiedFormula(quantifier, variableList, body) =>
        var map0 = subst
        var variableList0: Seq[String] = Seq.empty
        variableList.foreach { vari =>
          val newVar = nextVariable()
          map0 = map0 + (vari -> newVar)
          variableList0 = variableList0 :+ newVar
        }
        val body0 = fofFormula0(body, map0)
        FOF.QuantifiedFormula(quantifier, variableList0, body0)
      case FOF.UnaryFormula(connective, body) =>
        FOF.UnaryFormula(connective, fofFormula0(body, subst))
      case FOF.BinaryFormula(connective, left, right) =>
        val left0 = fofFormula0(left, subst)
        val right0 = fofFormula0(right, subst)
        FOF.BinaryFormula(connective, left0, right0)
      case FOF.Equality(left, right) =>
        val left0 = fofTerm0(left, subst)
        val right0 = fofTerm0(right, subst)
        FOF.Equality(left0, right0)
      case FOF.Inequality(left, right) =>
        val left0 = fofTerm0(left, subst)
        val right0 = fofTerm0(right, subst)
        FOF.Inequality(left0, right0)
    }
  }
  private def fofTerm0(term: TPTP.FOF.Term, subst: Map[String, String]): TPTP.FOF.Term = {
    term match {
      case FOF.AtomicTerm(f, args) =>
        FOF.AtomicTerm(f, args.map(fofTerm0(_, subst)))
      case FOF.Variable(name) =>
        subst.get(name) match {
          case Some(name0) => FOF.Variable(name0)
          case None => FOF.Variable(name) // Should not happen if formula/term is closed. Edge case
        }
      case _ => term
    }
  }

  private def tcfFormula(formula: TPTP.TCF.Formula): TPTP.TCF.Formula = {
    formula // TODO?
  }

  private def cnfFormula(formula: TPTP.CNF.Formula): TPTP.CNF.Formula = {
    formula // TODO?
  }
}

object CanonicalVariables {
  private final val variableBaseName: String = "X"
  @inline final private[this] def apply0(): CanonicalVariables = new CanonicalVariables()

  /**
   * Rename all (bound) variables in the formula in a canonical way:
   * All (bound) variables are renamed X1, X2, ..., XN in order of occurrence
   * when doing a pre-order traversal of the formula tree.
   * @param formula The formula to be normalized.
   * @return Canonically variable renamed formula.
   * @throws scala.IllegalArgumentException if a non-logical formula is passed
   */

  @inline final def apply(formula: TPTP.THF.Formula): TPTP.THF.Formula = apply0().thfFormula(formula)
  /** @see [[apply()]] */
  @inline final def apply(formula: TPTP.TFF.Formula): TPTP.TFF.Formula = apply0().tffFormula(formula)
  /** @see [[apply()]] */
  @inline final def apply(formula: TPTP.FOF.Formula): TPTP.FOF.Formula = apply0().fofFormula(formula)
  /** @see [[apply()]] */
  @inline final def apply(formula: TPTP.TCF.Formula): TPTP.TCF.Formula = apply0().tcfFormula(formula)
  /** @see [[apply()]] */
  @inline final def apply(formula: TPTP.CNF.Formula): TPTP.CNF.Formula = apply0().cnfFormula(formula)
  /** @see [[apply()]] */
  @inline final def apply(formula: TPTP.AnnotatedFormula#F): TPTP.AnnotatedFormula#F = apply0().apply(formula)
  /** @see [[apply()]] */
  @inline final def thfFormula(formula: TPTP.THF.Formula): TPTP.THF.Formula = apply0().thfFormula(formula)
  /** @see [[apply()]] */
  @inline final def tffFormula(formula: TPTP.TFF.Formula): TPTP.TFF.Formula = apply0().tffFormula(formula)
  /** @see [[apply()]] */
  @inline final def fofFormula(formula: TPTP.FOF.Formula): TPTP.FOF.Formula = apply0().fofFormula(formula)
  /** @see [[apply()]] */
  @inline final def tcfFormula(formula: TPTP.TCF.Formula): TPTP.TCF.Formula = apply0().tcfFormula(formula)
  /** @see [[apply()]] */
  @inline final def cnfFormula(formula: TPTP.CNF.Formula): TPTP.CNF.Formula = apply0().cnfFormula(formula)
}
