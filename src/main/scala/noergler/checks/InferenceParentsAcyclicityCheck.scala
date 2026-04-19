package noergler.checks

import leo.datastructures.TPTP

import scala.collection.mutable

object InferenceParentsAcyclicityCheck {
  final def apply(proofSteps: Seq[TPTP.FOFAnnotated], proofFormulas: Map[String, TPTP.FOFAnnotated], relaxAnnotationFormat:Boolean): Boolean = {
    if (proofSteps.isEmpty) true
    else {
      val lastStep = proofSteps.last
      dfs(proofFormulas, lastStep, mutable.Set.empty, mutable.Set.empty,relaxAnnotationFormat)
      }
  }

  private final def dfs(proofFormulas: Map[String, TPTP.FOFAnnotated],
                        proofStep: TPTP.FOFAnnotated,
                        visiting: scala.collection.mutable.Set[String],
                        done: scala.collection.mutable.Set[String],
                        relaxAnnotationFormat: Boolean): Boolean = {
    val stepName = proofStep.name
    if (done.contains(stepName)) true
    else if (visiting.contains(stepName)) false
    else {
      visiting += stepName
      val parentsOfStep = noergler.proofStepParents(proofStep, relaxAnnotationFormat)
      val result = parentsOfStep match {
        case Some(parentNames) =>
          parentNames.forall { parentName =>
            val parent = proofFormulas.get(parentName)
            parent match {
              case Some(parent0) =>
                dfs(proofFormulas, parent0, visiting, done, relaxAnnotationFormat)
              case None => true // it's acyclic because it does not exist!
            }
          }
        case None => true
      }
      visiting -= stepName
      if (result) done += stepName
      result
    }
  }
}
