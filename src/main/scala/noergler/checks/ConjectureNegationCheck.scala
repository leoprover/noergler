package noergler.checks

import leo.datastructures.TPTP
import leo.datastructures.TPTP.FOF

import java.util.logging.Logger

final class ConjectureNegationCheck(proofstep: TPTP.AnnotatedFormula,
                                    conjecture: TPTP.AnnotatedFormula) {
  val logger: Logger = Logger.getLogger("Nörgler.checks.ConjectureNegationCheck")

  def apply(): (Boolean, Option[TPTP.FOF.Formula]) = {
    conjecture.formula match {
      // todo: add other logics as well
      case FOF.Logical(conj) =>
        val manuallyNegatedConjecture: TPTP.FOF.Formula = TPTP.FOF.UnaryFormula(TPTP.FOF.~, conj)
        proofstep.formula match {
          case FOF.Logical(deFactoNegatedConjecture) =>
            logger.finer(s"Conjecture: ${conjecture.pretty}")
            logger.finer(s"Manually negated conjecture: ${manuallyNegatedConjecture.pretty}")
            logger.finer(s"negated conjecture from proof: ${deFactoNegatedConjecture.pretty}")

            val manualNF: TPTP.FOF.Formula = leo.modules.tptputils.Normalization.PrenexNormalform.normalizeFOFFormula(manuallyNegatedConjecture)
            val deFactoNF: TPTP.FOF.Formula = leo.modules.tptputils.Normalization.PrenexNormalform.normalizeFOFFormula(deFactoNegatedConjecture)

            logger.fine(s"Manually negated conjecture NF: ${manualNF.pretty}")
            logger.fine(s"negated conjecture from proof NF: ${deFactoNF.pretty}")
            val correctNegation = manualNF == deFactoNF
            val correctRole = proofstep.role == "negated_conjecture"

            if (correctRole) {
              if (correctNegation) (true, None) // formulas are identical -> will be accepted
              else (false, Some(manuallyNegatedConjecture)) // potentially test for entailment
            } else (false, None) // Negated conjecture has wrong role -> Reject without potential further tests
          //correctNegation && correctRole
        }
    }
  }
}
object ConjectureNegationCheck {
//  final def apply(proofstep: TPTP.FOFAnnotated,
//                  conjecture: Option[TPTP.FOFAnnotated]): (Boolean, Option[TPTP.FOF.Formula]) = {
//    conjecture.fold((false, None : Option[TPTP.FOF.Formula]))(new ConjectureNegationCheck(proofstep, _).apply())

  final def apply(proofstep: TPTP.AnnotatedFormula,
                  conjecture: Option[TPTP.AnnotatedFormula]): (Boolean, Option[TPTP.FOF.Formula]) = {
    conjecture.fold(false, None : Option[TPTP.FOF.Formula])(new ConjectureNegationCheck(proofstep, _).apply())

  }
}
