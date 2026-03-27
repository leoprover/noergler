package noergler.checks

import leo.datastructures.TPTP

object InferenceParentsAcyclicityCheck {
  final def apply(proofSteps: Seq[TPTP.FOFAnnotated], proofFormulas: Map[String, TPTP.FOFAnnotated]): Boolean = {
    if (proofSteps.isEmpty) true
    else {
      val lastStep = proofSteps.last
      dfs(proofSteps, proofFormulas, lastStep, Set.empty)
      }
  }

  private final def dfs(proofSteps: Seq[TPTP.FOFAnnotated],
                        proofFormulas: Map[String, TPTP.FOFAnnotated],
                        proofStep: TPTP.FOFAnnotated,
                        visitedNames: Set[String]): Boolean = {
    if (visitedNames.contains(proofStep.name)) false
    else {
      val parentsOfStep = noergler.proofStepParents(proofStep)
      parentsOfStep match {
        case Some(parentNames) =>
          parentNames.forall { parentName =>
            val parent = proofFormulas.get(parentName)
            parent match {
              case Some(parent0) =>
                dfs(proofSteps, proofFormulas, parent0, visitedNames + proofStep.name)
              case None => true // it's acyclic because it does not exist!
            }
          }
        case None => true
      }
    }
  }
}
