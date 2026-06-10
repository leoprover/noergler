package noergler.checks

import leo.datastructures.TPTP
import leo.datastructures.TPTP.{CNF, FOF, TCF, TFF, THF}
import noergler.{annotationType, proofStepParentsAsFormulas}

object ProofEndsInFalseCheck {
  @inline private[this] final val falseFormula = "$false"

  final def isFalse(step: TPTP.AnnotatedFormula): Boolean = {
    step match {
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
          case _ => false
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
  }

  final def findSinks(proofSteps: Seq[TPTP.AnnotatedFormula],
                      proofFormulas: Map[String, TPTP.AnnotatedFormula],
                      relaxAnnotationFormat: Boolean): Option[Set[TPTP.AnnotatedFormula]] = {
    // every formula that is an inference and has no children is a sink.
    var parents: Set[TPTP.AnnotatedFormula] = Set.empty // accumulator of all formulas that are parents of any other formula
    var inferences: Set[TPTP.AnnotatedFormula] = Set.empty // accumulator of all inferences in the given file

    proofSteps.foreach { proofStep =>
      val annotation = proofStep.annotations
      annotationType(annotation) match {
        case Some(annotationType) => annotationType match {
          case "inference" =>
            inferences = inferences + proofStep
            val currentParents = proofStepParentsAsFormulas(proofStep, proofFormulas, relaxAnnotationFormat)
            if (currentParents.isDefined) parents = parents ++ currentParents.get
            else return None // sinks could not be identified due to malformed annotation format
          case _ => //don't care
        }
        case None => // don't care
      }
    }

    Some(inferences.diff(parents))
  }

  final def apply(proof: TPTP.Problem,
                  proofFormulas: Map[String, TPTP.AnnotatedFormula],
                  relaxAnnotationFormat: Boolean,
                  enforceProofOrder: Boolean): Option[Boolean] = {
    if (proof.formulas.nonEmpty) {
      if (enforceProofOrder){
        // textual proof order is enforced -> actual last step needs to be false
        val lastProofStep = proof.formulas.last
        Some(isFalse(lastProofStep))
      } else {
        // last step in the DGA needs to be false
        // current policy: one sink that is false is sufficient
        val sinks0 = findSinks(proof.formulas,proofFormulas, relaxAnnotationFormat)
        sinks0 match {
          case Some(sinks) => Some(sinks.exists(isFalse))
          case None => None
        }
      }
    } else Some(false)
  }
}
