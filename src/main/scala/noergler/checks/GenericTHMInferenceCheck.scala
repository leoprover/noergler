package noergler.checks

import leo.datastructures.TPTP

class GenericTHMInferenceCheck {

}
object GenericTHMInferenceCheck {
  final def apply(proofstep: TPTP.FOFAnnotated,
                  parents: Seq[TPTP.FOFAnnotated]): Boolean = {
    false
  }
}
