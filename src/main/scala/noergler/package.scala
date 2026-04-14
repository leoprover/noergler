import leo.datastructures.TPTP

package object noergler {
  sealed abstract class Result
  final case object Verified extends Result
  final case class FailedVerified(reason: String) extends Result
  final case class NotVerified(reason: String) extends Result

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

  @inline final def proofStepParentsAsFormulas(proofstep: TPTP.FOFAnnotated,
                                               proofFormulas: Map[String, TPTP.FOFAnnotated],
                                               relaxAnnotationFormat: Boolean): Option[Seq[TPTP.FOFAnnotated]] = {
    val parents = proofStepParents(proofstep,relaxAnnotationFormat)
    parents.flatMap { parents =>
      val asFormulas = parents.map(proofFormulas.get)
      if (asFormulas.forall(_.isDefined)) Some(asFormulas.flatten)
      else None
    }
  }

  @inline final def proofStepParents(proofStep: TPTP.FOFAnnotated, relaxAnnotationFormat: Boolean): Option[Seq[String]] = {
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

}
