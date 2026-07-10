import leo.datastructures.TPTP

package object noergler {
  sealed abstract class Result
  final case object VerifiedGood extends Result
  final case class VerifiedBad(reason: String) extends Result
  final case class VerifiedUnknown(reason: String) extends Result
  final case class VerifiedTimeout(system: String) extends Result

  final def annotationType(annotation: TPTP.Annotations): Option[String] = {
    annotation match {
      case Some(annotation0) =>
        val gt = annotation0._1
        if (gt.data.nonEmpty) {
          gt.data.head match {
            case TPTP.MetaFunctionData(name, _) => Some(name)
            case _ => None
          }
        } else None
      case None => None
    }
  }

  final def parentsContain(proofstep: TPTP.AnnotatedFormula,
                           conjectureName: String,
                           relaxAnnotationFormat: Boolean): Boolean = {
    val parentsOfStep = noergler.proofStepParents(proofstep, relaxAnnotationFormat)
    parentsOfStep match {
      case Some(parentNames) =>
        // names in annotations may be enclosed in single quotes while they are stripped from formula names during parsing
        val stippedParentNames = parentNames.map(stripQuotes)
        if (stippedParentNames.nonEmpty) stippedParentNames.contains(conjectureName)
        else false
      case None => true
    }
  }

  final def constructInferenceAnnotation(ruleName: String, parents: Seq[String]): TPTP.Annotations = {
    val nameTerm = TPTP.GeneralTerm(Seq(TPTP.MetaFunctionData(ruleName, Seq.empty)), None)

    val statusInner = TPTP.GeneralTerm(Seq(TPTP.MetaFunctionData("thm", Seq.empty)), None)
    val statusMeta = TPTP.MetaFunctionData("status", Seq(statusInner))
    val statusEntry = TPTP.GeneralTerm(Seq(statusMeta), None)
    val statusTerm = TPTP.GeneralTerm(Seq.empty, Some(Seq(statusEntry)))

    val parentTerms = parents.map { p =>
      TPTP.GeneralTerm(Seq(TPTP.MetaFunctionData(p, Seq.empty)), None)
    }
    val parentsTerm = TPTP.GeneralTerm(Seq.empty, Some(parentTerms))

    val inferenceData = TPTP.MetaFunctionData(
      "inference",
      Seq(nameTerm, statusTerm, parentsTerm)
    )

    val finalGt = TPTP.GeneralTerm(Seq(inferenceData), None)
    Some((finalGt, None: Option[Seq[TPTP.GeneralTerm]]))
  }

  final def fileRecord(annotation: TPTP.Annotations): Option[(String, String)] = {
    annotation match {
      case Some(annotation0) =>
        val gt = annotation0._1
        if (gt.data.nonEmpty) {
          gt.data.head match {
            case TPTP.MetaFunctionData("file", args) if args.size == 2 =>
              val filenamePart = args.head
              val formulaNamePart = args.tail.head
              filenamePart.data match {
                case Seq(TPTP.MetaFunctionData(filename, Seq())) =>
                  formulaNamePart.data match {
                    case Seq(TPTP.MetaFunctionData(formulaName, Seq())) =>
                      Some((filename, formulaName))
                    case _ => None
                  }
                case _ => None
              }
            case _ => None
          }
        } else None
      case None => None
    }
  }

  final def inferenceName(annotation: TPTP.Annotations): Option[String] = {
    annotation match {
      case Some(annotation0) =>
        val gt = annotation0._1
        if (gt.data.nonEmpty) {
          gt.data.head match {
            case TPTP.MetaFunctionData("inference", args) if args.nonEmpty => args.head.data match {
              case Seq(TPTP.MetaFunctionData(inferenceName, Seq())) => Some(inferenceName)
              case _ => None
            }
            case _ => None
          }
        } else None
      case None => None
    }
  }

  sealed abstract class InferenceStatus
  /** Theorem */
  final case object THM extends InferenceStatus
  /** Counter-theorem */
  final case object CTH extends InferenceStatus
  /** Equisatisfiable */
  final case object ESA extends InferenceStatus
  final case class OtherStatus(name: String) extends InferenceStatus

  final def inferenceStatus(annotation: TPTP.Annotations): Option[InferenceStatus] = {
    annotation match {
      case Some(annotation0) =>
        val gt = annotation0._1
        if (gt.data.nonEmpty) {
          gt.data.head match {
            case TPTP.MetaFunctionData("inference", args) if args.size >= 2 => args.tail.head.list match {
              case Some(entries) =>
                val statusEntry = entries.find { entry =>
                  entry.data match {
                    case Seq(TPTP.MetaFunctionData("status", Seq(_))) => true
                    case _ => false
                  }
                }
                statusEntry.flatMap { entry =>
                  entry.data match {
                    case Seq(TPTP.MetaFunctionData("status", Seq(gt1))) => gt1.data match {
                      case Seq(TPTP.MetaFunctionData(status, Seq())) =>
                        status match {
                          case "thm" => Some(THM)
                          case "cth" => Some(CTH)
                          case "esa" => Some(ESA)
                          case _ => Some(OtherStatus(status))
                        }
                      case _ => None
                    }
                    case _ => None
                  }
                }
              case _ => None
            }
            case _ => None
          }
        } else None
      case None => None
    }
  }

  @inline final def proofStepParentsAsFormulas(proofstep: TPTP.AnnotatedFormula,
                                               proofFormulas: Map[String, TPTP.AnnotatedFormula],
                                               relaxAnnotationFormat: Boolean): Option[Seq[TPTP.AnnotatedFormula]] = {
    val parents = proofStepParents(proofstep,relaxAnnotationFormat)
    parents.flatMap { parents =>
      val asFormulas = parents.map(proofFormulas.get)
      if (asFormulas.forall(_.isDefined)) Some(asFormulas.flatten)
      else None
    }
  }

  @inline final def proofStepParents(proofStep: TPTP.AnnotatedFormula, relaxAnnotationFormat: Boolean): Option[Seq[String]] = {
    proofStepParents(proofStep.annotations, relaxAnnotationFormat)
  }

  final def proofStepParents0(gt: TPTP.GeneralData, relaxAnnotationFormat: Boolean): Option[Seq[String]] = {
    gt match {
      case TPTP.MetaFunctionData(name, args) if args.size >= 3 && Seq("inference", "introduced").contains(name) => args.tail.tail.head.list match {
        case Some(parentsAnnotation) =>
          val result = parentsAnnotation.flatMap(an => inferenceParent0(an, relaxAnnotationFormat))
          if (result.size == parentsAnnotation.size) Some(result.flatten)
          else None // Not well-formed
        case _ => None
      }
      case TPTP.MetaFunctionData("file", _) => Some(Seq.empty)
      case _ => None
    }
  }

  /** Returns a list of inference parents' names, if any.
   * Empty list of parents is returned as Some(Seq()), None is an error case. */
  final def proofStepParents(annotation: TPTP.Annotations, relaxAnnotationFormat: Boolean): Option[Seq[String]] = {
    annotation match {
      case Some(annotation0) =>
        val gt = annotation0._1
        if (gt.data.nonEmpty) {
          proofStepParents0(gt.data.head, relaxAnnotationFormat)
        } else None
      case None => None
    }
  }
  private final def inferenceParent0(parentAnnotation: TPTP.GeneralTerm, relaxAnnotationFormat:Boolean): Option[Seq[String]] = {
    if (parentAnnotation.list.isDefined) None
    else {
      parentAnnotation.data match {
        case Seq(TPTP.MetaFunctionData(parentName, Seq())) =>
          val strippedParentName = stripQuotes(parentName)
          Some(Seq(strippedParentName))
        case Seq(TPTP.NumberData(number)) => Some(Seq(number.pretty))
        // nested application of inference
        case Seq(TPTP.MetaFunctionData("inference", s)) if (relaxAnnotationFormat) => {
          val parentSeq = proofStepParents0(TPTP.MetaFunctionData("inference", s),relaxAnnotationFormat)
          parentSeq
        }
        case _ => None
      }
    }
  }

  final def metaFunctionDataToFOF(data: TPTP.GeneralData): Option[TPTP.FOF.Term] = {
    data match {
      case TPTP.MetaFunctionData(f, args) =>
        val res = args.map {
          case TPTP.GeneralTerm(Seq(entry), None) => metaFunctionDataToFOF(entry)
          case _ => None
        }
        if (res.exists(_.isEmpty)) None
        else Some(TPTP.FOF.AtomicTerm(f, res.flatten))
      case TPTP.MetaVariable(variable) => Some(TPTP.FOF.Variable(variable))
      case TPTP.NumberData(number) => Some(TPTP.FOF.NumberTerm(number))
      case TPTP.DistinctObjectData(name) => Some(TPTP.FOF.DistinctObject(name))
      case TPTP.GeneralFormulaData(TPTP.FOTData(term)) => Some(term)
      case _ => None
    }
  }

  @inline final def stripQuotes(name: String): String = {
    if (name.startsWith("'") && name.endsWith("'"))
      name.tail.init
    else name
  }

  final def runSimpleCommand(cmd: String): (Seq[String], Seq[String]) = {
    import scala.sys.process.{Process, ProcessLogger}
    val out: collection.mutable.Buffer[String] = collection.mutable.Buffer.empty
    val err: collection.mutable.Buffer[String] = collection.mutable.Buffer.empty
    Process(cmd) ! ProcessLogger(out append _, err append _)
    (out.toSeq, err.toSeq)
  }

}
