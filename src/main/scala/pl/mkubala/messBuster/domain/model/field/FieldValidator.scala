package pl.mkubala.messBuster.domain.model.field

import scala.xml.Node
import pl.mkubala.xml.NodeAttributesReader
import pl.mkubala.messBuster.domain.model.hook.Hook

case class FieldValidator(validatorType: String, hasCorrectFieldType: Boolean, message: Option[String], params: ValidatorParams)

object FieldValidator extends NodeAttributesReader {

  val validatesWith = "validatesWith"
  val validatesLength = "validatesLength"
  val validatesScale = "validatesScale"
  val validatesUnscaledValue = "validatesUnscaledValue"
  val validatesRange = "validatesRange"
  val validatesRegex = "validatesRegex"

  implicit def nodeToFieldValidator(node: Node)(implicit fieldType: FieldType) = {
    FieldValidator(node)
  }

  def apply(node: Node)(implicit fieldType: FieldType): FieldValidator = {

    val params = node.label match {
      case "validatesWith" => CustomValidatorParams(
        node.getStringAttr("class").get,
        node.getStringAttr("method").get)
      case "validatesLength" | "validatesScale" | "validatesUnscaledValue" => LengthValidatorParams(
        node.getIntAttr("min"),
        node.getIntAttr("is"),
        node.getIntAttr("max"))
      case "validatesRange" => RangeValidatorParams(
        node.getIntAttr("from"),
        node.getIntAttr("to"),
        node.getBooleanAttr("exclusively").get)
      case "validatesRegex" => RegexValidatorParams(node.getStringAttr("pattern").get)
      case validatorType => throw new IllegalArgumentException("unsupported validator type: '%s'".format(validatorType))
    }

    FieldValidator(node.label, fieldType.isHookCompatible(node.label), node.getStringAttr("message"), params)
  }

}

trait ValidatorParams {
  def isUpperBounded = false
}
case class CustomValidatorParams(hookClass: String, hookMethod: String) extends ValidatorParams with Hook
case class LengthValidatorParams(min: Option[Int] = None, is: Option[Int] = None, max: Option[Int] = None) extends ValidatorParams {
  override def isUpperBounded = is.isDefined || max.isDefined
}
case class RangeValidatorParams(from: Option[Int] = None, to: Option[Int] = None, exclusively: Boolean = false) extends ValidatorParams {
  override def isUpperBounded = to.isDefined
}
case class RegexValidatorParams(pattern: String) extends ValidatorParams
