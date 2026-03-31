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
                                               proofFormulas: Map[String, TPTP.FOFAnnotated]): Option[Seq[TPTP.FOFAnnotated]] = {
    val parents = proofStepParents(proofstep)
    parents.flatMap { parents =>
      val asFormulas = parents.map(proofFormulas.get)
      if (asFormulas.forall(_.isDefined)) Some(asFormulas.flatten)
      else None
    }
  }

  @inline final def proofStepParents(proofStep: TPTP.FOFAnnotated): Option[Seq[String]] = {
    proofStepParents(proofStep.annotations)
  }

  /** Returns a list of inference parents' names, if any.
   * Empty list of parents is returned as Some(Seq()), None is an error case. */
  final def proofStepParents(annotation: TPTP.Annotations): Option[Seq[String]] = {
    annotation match {
      case Some(annotation0) =>
        val gt = annotation0._1
        if (gt.data.nonEmpty) {
          gt.data.head match {
            case TPTP.MetaFunctionData(name, args) if args.size >= 3 && Seq("inference", "introduced").contains(name) => args.tail.tail.head.list match {
              case Some(parentsAnnotation) =>
                val result = parentsAnnotation.flatMap(inferenceParent0)
                if (result.size == parentsAnnotation.size) Some(result)
                else None // Not well-formed
              case _ => None
            }
            case TPTP.MetaFunctionData("file", _) => Some(Seq.empty)
            case _ => None
          }
        } else None
      case None => None
    }
  }
  private final def inferenceParent0(parentAnnotation: TPTP.GeneralTerm): Option[String] = {
    if (parentAnnotation.list.isDefined) None
    else {
      parentAnnotation.data match {
        case Seq(TPTP.MetaFunctionData(parentName, Seq())) => Some(parentName)
        case _ => None
      }
    }
  }
}
