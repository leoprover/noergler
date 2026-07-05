package noergler.checks

import leo.datastructures.TPTP
import noergler.{parentsContain}

object InferenceParentsAreNotConjecture {
  final def apply(proofstep: TPTP.AnnotatedFormula,
                  conjectureName: Option[String],
                  relaxAnnotationFormat: Boolean): Boolean = {
    if (conjectureName.isDefined) {
      !parentsContain(proofstep, conjectureName.get, relaxAnnotationFormat)
    } else true
  }
}