package noergler.checks

import leo.datastructures.TPTP
import noergler.{fileRecord, stripQuotes}

import java.util.logging.Logger

object CorrectFormulaFromFileCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.checks.CorrectFormulaFromFileCheck")

  final def apply(proofstep: TPTP.FOFAnnotated,
                  problemFileName: String,
                  problemFormulas: Map[String, TPTP.FOFAnnotated],
                  relaxProblemCheck: Boolean): Boolean = {
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
          val rolesMatch: Boolean = if (formulaFromProblem.role == proofstep.role) {
            true
          } else {
            if (relaxProblemCheck) {
              logger.info(s"Roles of formula $formulaName do not match in problem (${formulaFromProblem.role}) and proof (${proofstep.role})")
              true
            } else false
          }
          logger.finest(s"Roles match: $rolesMatch")
          val formulasMatch = formulaFromProblem.formula == proofstep.formula
          logger.finest(s"formulas match: $formulasMatch")
          fileNamesMatch && rolesMatch && formulasMatch
        }
      case None => false
    }
  }
}
