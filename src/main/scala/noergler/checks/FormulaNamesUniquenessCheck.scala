package noergler.checks

import leo.datastructures.TPTP

object FormulaNamesUniquenessCheck {
  import scala.collection.mutable
  final def apply(proofSteps: Seq[TPTP.FOFAnnotated]): Option[String] = {
    var doubleTakenName: Option[String] = None
    var soFarSoGood = true
    val usedNames: mutable.Set[String] = mutable.Set.empty
    val proofStepsIt = proofSteps.iterator
    while (proofStepsIt.hasNext && soFarSoGood) {
      val proofStep = proofStepsIt.next()
      val name = proofStep.name
      if (usedNames.contains(name)) {
        soFarSoGood = false
        doubleTakenName = Some(name)
      } else usedNames.addOne(name)
    }
    doubleTakenName
  }
}