package noergler.checks

import leo.datastructures.TPTP
import noergler.normalization.CanonicalVariables
import noergler.{fileRecord, stripQuotes}

import java.nio.file.{InvalidPathException, Path}
import java.util.logging.Logger

object CorrectFormulaFromFileCheck {
  final val logger: Logger = Logger.getLogger("Nörgler.checks.CorrectFormulaFromFileCheck")

  /**
   * Run the check.
   * @return `None` if check succeeds, `Some(err)` if check fails, where
   *         `err` is a String describung why the check failed.
   */
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
              // (1) Check that the path in the file(...) annotation matches the problem file
              val fullPathReconstructedFromFormulaAnnotation = proofPath.getParent.resolve(stripQuotes(file)).toAbsolutePath.normalize()
              logger.finer(s"fullPathReconstructedFromFormulaAnnotation = ${fullPathReconstructedFromFormulaAnnotation.toString}")
              if (problemPath.normalize() != fullPathReconstructedFromFormulaAnnotation) {
                @inline val msg = s"Problem file path does not match the file annotation in proof step '${proofstep.name}'."
                logger.finer(msg)
                return Some(msg)
              }
              // (2) Check that the role of the imported formula is identical to the role from the problem file
              if (formulaFromProblem.role != proofstep.role) {
                @inline val msg = s"Role of proof step ${proofstep.name} (${proofstep.role}) do not match role of formula $formulaName from problem file (${formulaFromProblem.role})."
                logger.info(msg)
                if (relaxProblemCheck) logger.info("Continuing check, because option --relax-annotation-format is set.")
                else return Some(msg)
              }
              // (3) Check that only certain roles can be taken from the problem file (e.g., plain would not be allowed)
              if (!Seq("axiom", "conjecture", "negated_conjecture", "type", "definition").contains(proofstep.role)) {
                @inline val msg = s"Only formulas of role 'axiom', 'conjecture', 'negated_conjecture', 'type', or 'definition' may be taken from a problem file, but role '${proofstep.role}' was given in proof step ${proofstep.name}."
                logger.info(msg)
                return Some(msg)
              }
              // (4) Check that the formula itself is identical (up to renaming of variables) to the one from the problem file.
              val canonicalFormulaFromProblem = CanonicalVariables.apply(formulaFromProblem.formula)
              val canonicalFormulaFromProofstep = CanonicalVariables.apply(proofstep.formula)
              logger.fine(s"canonicalFormulaFromProblem = ${canonicalFormulaFromProblem.pretty}")
              logger.fine(s"canonicalFormulaFromProofstep = ${canonicalFormulaFromProofstep.pretty}")
              if (canonicalFormulaFromProblem != canonicalFormulaFromProofstep) {
                @inline val msg = s"Formula in proof step ${proofstep.name} is not identical (up to renaming) to formula $formulaName from problem file."
                logger.finer(msg)
                return Some(msg)
              }
              None
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
