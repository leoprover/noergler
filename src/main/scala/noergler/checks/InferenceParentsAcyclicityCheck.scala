package noergler.checks

import leo.datastructures.TPTP

object InferenceParentsAcyclicityCheck {
  final def apply(proofSteps: Seq[TPTP.FOFAnnotated], proofFormulas: Map[String, TPTP.FOFAnnotated]): Boolean = {
    false
  }
}
