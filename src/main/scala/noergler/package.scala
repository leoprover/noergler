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

  final def fileRecord(annotation: (TPTP.GeneralTerm, Option[Seq[TPTP.GeneralTerm]])): Option[(String, String)] = {
    ???
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

  final def inferenceStatus(annotation: TPTP.Annotations): Option[String] = {
    annotation match {
      case Some(annotation0) =>
        val gt = annotation0._1
        if (gt.data.nonEmpty) {
          gt.data.head match {
            case TPTP.MetaFunctionData("inference", args) if args.size >= 2 => args.tail.head.list match {
              case Some(Seq(gt0)) => gt0.data match {
                case Seq(TPTP.MetaFunctionData("status", Seq(gt1))) => gt1.data match {
                  case Seq(TPTP.MetaFunctionData(status, Seq())) => Some(status)
                  case _ => None
                }
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
