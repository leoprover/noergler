package noergler.checks

import leo.datastructures.TPTP

class SkolemizationCheck {

}
object SkolemizationCheck {
  final def apply(proofstep: TPTP.FOFAnnotated,
                  otherStuff: AnyVal): Boolean = { // TODO: What information do we need here?
    // inference parents (non-skolemized formula), but also list of already used names?
    false
  }
}
