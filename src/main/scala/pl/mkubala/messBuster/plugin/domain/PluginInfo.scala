package pl.mkubala.messBuster.plugin.domain

import scala.collection.immutable.Seq
import scala.xml.Node

case class PluginInfo(
  group: String = "",
  isSystem: Boolean = false,
  version: String = "",
  name: String = "",
  description: String = "",
  license: String = "",
  dependencies: Seq[String] = Vector())

object PluginInfo {

  private val getAttr = (node: Node, attrName: String) =>
    node.attribute(attrName).map(_.text).getOrElse("")

  def apply(descriptor: PluginDescriptor): PluginInfo = {
    val informationNode = descriptor.xml \ "information"
    val name = (informationNode \ "name").text
    val description = (informationNode \ "description").text
    val license = (informationNode \ "license").text

    val group = getAttr(descriptor.xml, "group")

    val version = getAttr(descriptor.xml, "version")

    val dependencies = (descriptor.xml \ "dependencies" \ "dependency").map(d => (d \ "plugin").text)

    val isSystem = descriptor.xml.attribute("system") match {
      case Some(value) => value.text.toBoolean
      case None => false
    }

    PluginInfo(group, isSystem, version, name, description, license, dependencies)
  }

}
