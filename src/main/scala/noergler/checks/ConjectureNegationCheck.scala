package noergler.checks

import leo.datastructures.TPTP
import leo.datastructures.TPTP.FOF

import java.util.logging.Logger

final class ConjectureNegationCheck(proofstep: TPTP.AnnotatedFormula,
                                    conjecture: TPTP.AnnotatedFormula) {
  val logger: Logger = Logger.getLogger("Nörgler.checks.ConjectureNegationCheck")

  def apply(): Boolean = {
    conjecture.formula match {
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
            correctNegation && correctRole
        }
    }

  }
}
object ConjectureNegationCheck {
  final def apply(proofstep: TPTP.AnnotatedFormula,
                  conjecture: Option[TPTP.AnnotatedFormula]): Boolean = {
    conjecture.fold(false)(new ConjectureNegationCheck(proofstep, _).apply())
  }
}
