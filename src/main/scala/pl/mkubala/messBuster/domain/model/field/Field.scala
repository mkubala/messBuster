package pl.mkubala.messBuster.domain.model.field

import pl.mkubala.messBuster.plugin.domain.PluginDescriptor
import scala.util.Try
import pl.mkubala.messBuster.model.domain._
import com.typesafe.scalalogging.slf4j.Logging
import scala.xml.{ Utility, Node }
import pl.mkubala.MessBuster.CanInject
import pl.mkubala.messBuster.parser._
import scala.xml.Utility._
import pl.mkubala.messBuster.model.domain.InjectorPlugin
import scala.util.Failure
import scala.Some
import scala.util.Success
import pl.mkubala.messBuster.domain.model.{ Model, ModelIdentifier }
import scala.collection.immutable.Seq
import pl.mkubala.messBuster.parser.ParseResult.ParseResult
import pl.mkubala.messBuster.parser.ParseResult

case class Field(
  fieldType: FieldType,
  name: String,
  validators: Seq[FieldValidator],
  implicitValidators: Set[FieldValidator],
  injectorPlugin: Option[String])
    extends Injectable

object Field {

  implicit val canInjectFieldToModel = new CanInject[Model, Field] {
    override val f = (model: Model, field: Field) => model.copy(fields = field :: model.fields)
  }

  implicit val fieldParser: Parser[PluginDescriptor, ParseResult[(Model#Selector, Field)]] =
    ConcreteParser[ParseResult[(Model#Selector, Field)]] { pd: PluginDescriptor =>
      ParseResult.convert(ExternalFieldsParser.parseFields(pd))
    }

  def apply(node: Node)(implicit injector: Injector = EmptyInjectorPlugin): Field = {
    val trimmedNode = trim(node)
    val fieldName = ((trimmedNode \ "@name").text)
    implicit val fieldType = FieldType(node)
    val validators = trimmedNode.child map (FieldValidator(_))

    val implicitValidators = fieldType.defaultValidators filterNot {
      case FieldValidator(implicitValidatorType, true, _, implicitValidatorParams) => validators exists {
        case FieldValidator(validatorType, true, _, validatorParams) if (implicitValidatorType == validatorType) => validatorParams.isUpperBounded
        case _ => false
      }
      case _ => false
    }

    Field(fieldType, fieldName, validators.toList, implicitValidators, injector.identifier)
  }

}

trait FieldsParser {

  def parseFields(plugin: PluginDescriptor): Seq[Try[(ModelIdentifier, Field)]]

}

object ExternalFieldsParser extends FieldsParser with Logging {

  override def parseFields(plugin: PluginDescriptor): Seq[Try[(Model#Selector, Field)]] = {
    val parser = parse(plugin)(_)
    (plugin.xml \ "modules" \ "model-field") map parser
  }

  private def parse(plugin: PluginDescriptor)(node: Node): Try[(ModelIdentifier, Field)] = {
    val trimmedNode = Utility.trim(node)
    val targetPlugin = trimmedNode.attribute("plugin").filter(_.nonEmpty).map(_.head.text).getOrElse(plugin.identifier)
    val targetModelIdentifier = ModelIdentifier(targetPlugin, (trimmedNode \ "@model").text)
    if (trimmedNode.child.isEmpty) {
      Failure(new IllegalStateException(s"Error in ${plugin.identifier}: broken node found - \n\t$trimmedNode"))
    } else {
      implicit val injector = InjectorPlugin(Some(plugin.identifier))
      val injectedField = Field(trimmedNode.child.head)
      Success((targetModelIdentifier, injectedField))
    }
  }

}
