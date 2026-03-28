package noergler.checks

import leo.datastructures.TPTP

final class CorrectFormulaFromFileCheck(proofstep: TPTP.FOFAnnotated,
                                        problemFormulas: Map[String, TPTP.FOFAnnotated]) {
  def apply(): Boolean = {
    // FIXME: Now use annotations
    problemFormulas.get(proofstep.name) match {
      case Some(formulaFromProblem) =>
        formulaFromProblem.role == proofstep.role &&
          formulaFromProblem.formula == proofstep.formula
      case None => false
    }
  }
}
object CorrectFormulaFromFileCheck {
  final def apply(proofstep: TPTP.FOFAnnotated,
                  problemFormulas: Map[String, TPTP.FOFAnnotated]): Boolean = {
    new CorrectFormulaFromFileCheck(proofstep, problemFormulas).apply()
  }
}
