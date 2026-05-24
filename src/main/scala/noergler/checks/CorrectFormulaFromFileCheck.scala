package noergler.checks

import leo.datastructures.TPTP
import noergler.{fileRecord, stripQuotes}

import java.nio.file.{InvalidPathException, Path}
import java.util.logging.Logger

object CorrectFormulaFromFileCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.checks.CorrectFormulaFromFileCheck")

  final def apply(proofstep: TPTP.AnnotatedFormula,
                  proofPath: Path,
                  problemPath: Path,
                  problemFormulas: Map[String, TPTP.AnnotatedFormula],
                  relaxProblemCheck: Boolean): Option[String] = {
    val origin = fileRecord(proofstep.annotations)

    origin match {
      case Some((file, formulaName)) =>
        logger.finer(s"Given from annotation: file = $file, formulaName = $formulaName")
        logger.finer(s"De facto proof file: ${proofPath.toString}")
        logger.finer(s"De facto problem file: ${problemPath.toString}")
        logger.finer(s"De facto formula name of proofstep: ${proofstep.name}")
        problemFormulas.get(formulaName) match {
          case None =>
            @inline val msg = s"Formula with name $formulaName not found in problem's formulas"
            logger.finer(msg)
            Some(msg)
          case Some(formulaFromProblem) =>
            try {
              val fullPathReconstructedFromFormulaAnnotation = proofPath.getParent.resolve(stripQuotes(file)).toAbsolutePath.normalize()
              logger.finer(s"fullPathReconstructedFromFormulaAnnotation = ${fullPathReconstructedFromFormulaAnnotation.toString}")
              if (problemPath.normalize() == fullPathReconstructedFromFormulaAnnotation) {
                if (formulaFromProblem.role == proofstep.role) {
                  if (formulaFromProblem.formula == proofstep.formula) {
                    None
                  } else {
                    @inline val msg = s"Formula in proof step ${proofstep.name} is not identical to formula $formulaName from problem file."
                    logger.finer(msg)
                    Some(msg)
                  }
                } else {
                  @inline val msg = s"Role of proof step ${proofstep.name} (${proofstep.role}) do not match role of formula $formulaName from problem file (${formulaFromProblem.role})."
                  logger.info(msg)
                  if (relaxProblemCheck) {
                    logger.info("Continuing check, because option --relax-annotation-format is set.")
                    None
                  } else {
                    Some(msg)
                  }
                }
              } else {
                @inline val msg = s"Problem file path does not match the file annotation in proof step '${proofstep.name}'."
                logger.finer(msg)
                Some(msg)
              }
            } catch {
              case e:InvalidPathException =>
                @inline val msg = s"File annotation path of proof step ${proofstep.name} malformed: ${e.toString}"
                logger.finer(msg)
                Some(msg)
            }
        }
      case None =>
        @inline val msg = s"No (or malformed) origin information in file annotation in proof step '${proofstep.name}'"
        logger.finer(msg)
        Some(msg)
    }
  }
}
