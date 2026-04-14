package noergler.checks

import leo.datastructures.TPTP
import noergler.{fileRecord, stripQuotes}

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
        problemFormulas.get(formulaName).fold {
          logger.finer(s"Formula with name $formulaName not found in problem formulas")
          false
        } { formulaFromProblem =>
          val fileNamesMatch = problemFileName == stripQuotes(filename)
          logger.finest(s"File names match: $fileNamesMatch")
          val rolesMatch = formulaFromProblem.role == proofstep.role
          logger.finest(s"Roles match: $rolesMatch")
          val formulasMatch = formulaFromProblem.formula == proofstep.formula
          logger.finest(s"formulas match: $formulasMatch")
          fileNamesMatch && rolesMatch && formulasMatch
        }
      case None => false
    }
  }
}
