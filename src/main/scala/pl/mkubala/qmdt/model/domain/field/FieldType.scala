package pl.mkubala.qmdt.model.domain.field

import scala.xml.Node
import pl.mkubala.qmdt.model.domain.ModelIdentifier
import pl.mkubala.xml.NodeAttributesReader

import pl.mkubala.qmdt.model.domain.field.FieldValidator.nodeToFieldValidator

object FieldType extends NodeAttributesReader {

  private val commaSeparatorRegexp = """[ ]*,[ ]*""".r

  def apply(node: Node): FieldType = {

    def extractRelatedModelIdentifier = {
      val model = node.getStringAttr("model", Some("")).get
      val plugin = node.getStringAttr("plugin", Some("")).get
      ModelIdentifier(plugin, model)
    }

    val commonParams = FieldParameters(node)
    node.label match {
      case label @ "integer" => IntegerFieldType(label, commonParams)
      case label @ "string" => StringFieldType(label, commonParams, node.getStringAttr("expression"))
      case label @ "text" => TextFieldType(label, commonParams)
      case label @ "password" => PasswordFieldType(label, commonParams)
      case label @ "decimal" => DecimalFieldType(label, commonParams)
      case label @ ("datetime" | "date") => DateFieldType(label, commonParams)
      case label @ ("file" | "boolean") => DefaultFieldType(label, commonParams)
      case label @ "belongsTo" =>
        BelongsToFieldType(label, commonParams,
          extractRelatedModelIdentifier,
          node.getBooleanAttr("lazy", true).get)
      case label @ ("hasMany" | "tree" | "manyToMany") =>
        CollectionFieldType(
          label, commonParams,
          extractRelatedModelIdentifier,
          node.getStringAttr("joinField").get,
          node.getStringAttr("cascade", Some("nullify")).get)
      case label @ "enum" => {
        val values = node.getStringAttr("values") map (commaSeparatorRegexp.split(_).toSeq) getOrElse Seq()
        EnumFieldType(label, commonParams, values)
      }
      case label @ "dictionary" => DictionaryFieldType(label, commonParams, "someDictionary")
      case label @ "priority" => PriorityFieldType(label, Some("scope"))
    }
  }

}

trait FieldType {
  val name: String
  val parameters: FieldParameters

  protected implicit val fieldType = this

  protected val compatibleHooks = Set[String]()

  def isHookCompatible(hookName: String) = compatibleHooks.contains(hookName)

  lazy val defaultValidators = Set[FieldValidator]()
}

case class DefaultFieldType(
  name: String,
  parameters: FieldParameters) extends FieldType {

}

trait CharacterLikeFieldType extends FieldType {

  override protected val compatibleHooks = Set(
    "validatesWith",
    "validatesRegex",
    "validatesLength",
    "validatesRange")

  protected val maxLen: Int

  override lazy val defaultValidators: Set[FieldValidator] = Set(
    <validatesLength max={ maxLen.toString }/>)
}

case class TextFieldType(
  name: String,
  parameters: FieldParameters,
  expression: Option[String] = None) extends CharacterLikeFieldType {
  override protected val maxLen = 2048
}

case class PasswordFieldType(
  name: String,
  parameters: FieldParameters,
  expression: Option[String] = None) extends CharacterLikeFieldType {
  override protected val maxLen = 255
}

case class StringFieldType(
  name: String,
  parameters: FieldParameters,
  expression: Option[String] = None) extends CharacterLikeFieldType {
  override protected val maxLen = 255
}

case class IntegerFieldType(
  name: String,
  parameters: FieldParameters) extends FieldType {

  override protected val compatibleHooks = Set(
    "validatesWith",
    "validatesRegex",
    "validatesUnscaledValue",
    "validatesRange")

  override lazy val defaultValidators: Set[FieldValidator] = Set(
    <validatesUnscaledValue max="10"/>)
}

case class DecimalFieldType(
  name: String,
  parameters: FieldParameters) extends FieldType {

  override protected val compatibleHooks = Set(
    "validatesWith",
    "validatesRegex",
    "validatesUnscaledValue",
    "validatesScale",
    "validatesRange")

  override lazy val defaultValidators: Set[FieldValidator] = Set(
    <validatesUnscaledValue max="7"/>,
    <validatesScale max="5"/>)
}

case class DateFieldType(
  name: String,
  parameters: FieldParameters) extends FieldType {

  override protected val compatibleHooks = Set(
    "validatesWith",
    "validatesRegex",
    "validatesRange")
}

trait RelationFieldType extends FieldType {
  val relatedModel: ModelIdentifier
}

case class BelongsToFieldType(
  name: String,
  parameters: FieldParameters,
  relatedModel: ModelIdentifier,
  isLazy: Boolean = true) extends RelationFieldType

case class CollectionFieldType(
  name: String,
  parameters: FieldParameters,
  relatedModel: ModelIdentifier,
  joinField: String,
  cascade: String = "nullify" // TODO introduce CascadeType
  ) extends RelationFieldType

case class EnumFieldType(
  name: String,
  parameters: FieldParameters,
  values: Seq[String]) extends FieldType

case class PriorityFieldType(
  name: String,
  scope: Option[String] = None) extends FieldType {
  val parameters = FieldParameters()
}

case class DictionaryFieldType(
  name: String,
  parameters: FieldParameters,
  dictionary: String) extends FieldType

