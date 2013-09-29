package pl.mkubala.messBuster.model.domain.field

import scala.xml.Node
import scala.xml.Utility.trim
import pl.mkubala.messBuster.model.domain.Injectable
import scala.xml.NodeSeq.seqToNodeSeq
import pl.mkubala.messBuster.model.domain.Injector
import pl.mkubala.messBuster.model.domain.EmptyInjectorPlugin

case class Field(fieldType: FieldType, name: String, validators: Seq[FieldValidator], implicitValidators: Set[FieldValidator], injectorPlugin: Option[String]) extends Injectable

object Field {
  def apply(node: Node)(implicit injector: Injector = EmptyInjectorPlugin): Field = {
    val trimmedNode = trim(node)
    val fieldName = (trimmedNode \ "@name" text)
    implicit val fieldType = FieldType(node)
    val validators = trimmedNode.child map (FieldValidator(_))

    val implicitValidators = fieldType.defaultValidators filterNot {
      case FieldValidator(implicitValidatorType, true, _, implicitValidatorParams) => validators exists {
        case FieldValidator(validatorType, true, _, validatorParams) if (implicitValidatorType == validatorType) => validatorParams.isUpperBounded
        case _ => false
      }
      case _ => false
    }

    Field(fieldType, fieldName, validators, implicitValidators, injector.identifier)
  }

}
