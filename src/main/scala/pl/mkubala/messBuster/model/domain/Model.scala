package pl.mkubala.messBuster.model.domain

import scala.xml.Node
import scala.xml.Utility.trim
import scala.xml.XML.loadFile
import pl.mkubala.messBuster.plugin.domain.PluginDescriptor
import pl.mkubala.messBuster.model.domain.field.Field
import pl.mkubala.xml.NodeAttributesReader

case class Model(pluginIdentifier: String, name: String, fields: List[Field] = Nil, hooks: List[ModelHook] = Nil, attributes: ModelAttributes) {

  def withField(field: Field) =
    Model(pluginIdentifier, name, field :: fields, hooks, attributes)

  def withHook(hook: ModelHook) =
    Model(pluginIdentifier, name, fields, hook :: hooks, attributes)
}

case class ModelIdentifier(pluginIdentifier: String, modelName: String)

object QcadooModelParser {

  def buildModels(plugin: PluginDescriptor): Seq[Model] =
    (plugin.xml \ "modules" \ "model") map (buildModel(plugin, _))

  def buildModel(plugin: PluginDescriptor, modelElement: Node) = {
    val modelPath = plugin.resourcesPath + '/' + plugin.identifier + '/' + (modelElement \ "@resource" text)
    Model(plugin, trim(loadFile(modelPath)))
  }
}

object Model {

  def apply(plugin: PluginDescriptor, modelXmlRoot: Node): Model = {
    val modelName = (modelXmlRoot \ "@name").text
    val hooks = ((modelXmlRoot \ "hooks") flatMap (_.child map (ModelHook(_)))).toList
    val fields = ((modelXmlRoot \ "fields") flatMap (_.child map (Field(_)))).toList

    Model(plugin.identifier, modelName, fields, hooks, ModelAttributes(modelXmlRoot))
  }
}

case class ModelAttributes(
  deleteable: Boolean = true,
  insertable: Boolean = true,
  updatable: Boolean = true,
  activable: Boolean = true,
  auditable: Boolean = false,
  cacheable: Boolean = false,
  expression: Option[String] = None)

object ModelAttributes extends NodeAttributesReader {
  def apply(node: Node): ModelAttributes = {
    ModelAttributes(
      node.getBooleanAttr("deletable", default = true).get,
      node.getBooleanAttr("insertable", default = true).get,
      node.getBooleanAttr("updatable", default = true).get,
      node.getBooleanAttr("activable", default = true).get,
      node.getBooleanAttr("auditable").get,
      node.getBooleanAttr("cacheable").get,
      node.getStringAttr("expression"))
  }
}
