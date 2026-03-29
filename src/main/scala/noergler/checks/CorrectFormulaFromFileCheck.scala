package noergler.checks

import leo.datastructures.TPTP
import noergler.fileRecord

import java.util.logging.Logger

object CorrectFormulaFromFileCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.checks.CorrectFormulaFromFileCheck")

  final def apply(proofstep: TPTP.FOFAnnotated,
                  problemFileName: String,
                  problemFormulas: Map[String, TPTP.FOFAnnotated]): Boolean = {
    val origin = fileRecord(proofstep.annotations)
    origin match {
      case Some((filename, formulaName)) =>
        logger.finer(s"Given from annotation: filename = $filename, formulaName = $formulaName")
        logger.finer(s"De facto problem file name: $problemFileName")
        logger.finer(s"De facto formula name of proofstep: ${proofstep.name}")
        problemFormulas.get(formulaName).fold(false) { formulaFromProblem =>
          problemFileName == stripQuotes(filename) &&
            formulaFromProblem.role == proofstep.role &&
            formulaFromProblem.formula == proofstep.formula
        }
      case None => false
    }
  }

  @inline private[this] final def stripQuotes(filename: String): String = {
    if (filename.startsWith("'") && filename.endsWith("'"))
      filename.tail.init
    else filename
  }
}
