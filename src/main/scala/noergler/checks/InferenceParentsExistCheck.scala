package noergler.checks

import leo.datastructures.TPTP

object InferenceParentsExistCheck {
  final def apply(proofstep: TPTP.FOFAnnotated,
                  previousProofSteps: Seq[TPTP.FOFAnnotated],
                  relaxAnnotationFormat: Boolean): Boolean = {
    val parentsOfStep = noergler.proofStepParents(proofstep, relaxAnnotationFormat)
    parentsOfStep match {
      case Some(parentNames) =>
        parentNames.forall { parentName =>
          previousProofSteps.exists(_.name == parentName)
        }
      case None => false
    }
  }
}