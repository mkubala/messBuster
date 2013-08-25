package pl.mkubala.xml

import scala.xml.Node

trait NodeAttributesReader {

  implicit def nodeToAttributesExtractorWrapper(node: Node): NodeAttributesExtractorWrapper = new NodeAttributesExtractorWrapper(node)

  class NodeAttributesExtractorWrapper(node: Node) {
    def getStringAttr(name: String, default: Option[String] = None) = getAttr(name, _.text, default)

    def getIntAttr(name: String, default: Option[Int] = None) = getAttr(name, _.text.toInt, default)

    def getBooleanAttr(name: String, default: Boolean = false) = getAttr(name, _.text.toBoolean, Some(default))

    def getAttr[T](name: String, extractFunc: Seq[Node] => T, default: Option[T] = None): Option[T] =
      node.attribute(name) map extractFunc orElse default
  }

}

