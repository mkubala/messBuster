package pl.mkubala.messBuster.model.domain.field

import scala.xml.Node
import pl.mkubala.xml.NodeAttributesReader

case class FieldParameters(
  default: Option[String] = None,
  readonly: Boolean = false,
  persistent: Boolean = true,
  required: Boolean = false,
  unique: Boolean = false,
  copyable: Boolean = true)

object FieldParameters extends NodeAttributesReader {

  def apply(node: Node): FieldParameters = {
    val isDefaultCopyable = node.label match {
      case ("hasMany" | "tree" | "manyToMany") => false
      case _ => true
    }
    FieldParameters(
      node.getStringAttr("default"),
      node.getBooleanAttr("readonly").get,
      node.getBooleanAttr("persistent", true).get,
      node.getBooleanAttr("required").get,
      node.getBooleanAttr("unique").get,
      node.getBooleanAttr("copyable", isDefaultCopyable).get)
  }
}