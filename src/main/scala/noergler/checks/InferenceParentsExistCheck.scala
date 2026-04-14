package noergler.checks

import leo.datastructures.TPTP
import noergler.stripQuotes

object InferenceParentsExistCheck {
  final def apply(proofstep: TPTP.FOFAnnotated,
                  previousProofSteps: Seq[TPTP.FOFAnnotated],
                  relaxAnnotationFormat: Boolean): Boolean = {
    val parentsOfStep = noergler.proofStepParents(proofstep, relaxAnnotationFormat)
    parentsOfStep match {
      case Some(parentNames) =>
        // names in annotations may be enclosed in single quotes while they are stripped from formula names during parsing
        val stippedParentNames = parentNames.map(stripQuotes)
        stippedParentNames.forall { parentName =>
          previousProofSteps.exists(_.name == parentName)
        }
      case None => false
    }
  }
}