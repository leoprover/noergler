package noergler.checks

import leo.datastructures.TPTP

object FormulaNamesUniquenessCheck {
  import scala.collection.mutable
  final def apply(proofSteps: Seq[TPTP.FOFAnnotated]): Boolean = {
    var soFarSoGood = true
    val usedNames: mutable.Set[String] = mutable.Set.empty
    val proofStepsIt = proofSteps.iterator
    while (proofStepsIt.hasNext && soFarSoGood) {
      val proofStep = proofStepsIt.next()
      val name = proofStep.name
      if (usedNames.contains(name)) soFarSoGood = false
      usedNames.addOne(name)
    }
    soFarSoGood
  }
}