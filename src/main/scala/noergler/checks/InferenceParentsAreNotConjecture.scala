package noergler.checks

import leo.datastructures.TPTP
import noergler.stripQuotes

object InferenceParentsAreNotConjecture {
  final def apply(proofstep: TPTP.FOFAnnotated,
                  conjectureName: Option[String],
                  relaxAnnotationFormat: Boolean): Boolean = {
    if (conjectureName.isDefined){
      val parentsOfStep = noergler.proofStepParents(proofstep, relaxAnnotationFormat)
      parentsOfStep match {
        case Some(parentNames) =>
          // names in annotations may be enclosed in single quotes while they are stripped from formula names during parsing
          val stippedParentNames = parentNames.map(stripQuotes)
          if (stippedParentNames.nonEmpty) !stippedParentNames.contains(conjectureName.get)
          else true
        case None => false
      }
    } else true
  }
}