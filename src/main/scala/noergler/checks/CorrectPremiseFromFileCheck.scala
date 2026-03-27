package noergler.checks

import leo.datastructures.TPTP

final class CorrectPremiseFromFileCheck(proofstep: TPTP.FOFAnnotated,
                                  problemFormulas: Map[String, TPTP.FOFAnnotated]) {
  def apply(): Boolean = {
    problemFormulas.get(proofstep.name) match {
      case Some(formulaFromProblem) =>
        formulaFromProblem.role == proofstep.role &&
          formulaFromProblem.formula == proofstep.formula
      case None => false
    }
  }
}
object CorrectPremiseFromFileCheck {
  final def apply(proofstep: TPTP.FOFAnnotated,
                  problemFormulas: Map[String, TPTP.FOFAnnotated]): Boolean = {
    new CorrectPremiseFromFileCheck(proofstep, problemFormulas).apply()
  }
}
