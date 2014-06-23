package pl.mkubala.messBuster.domain.model

import scala.xml.Node
import scala.xml.Utility.trim
import scala.xml.XML.loadFile
import pl.mkubala.messBuster.plugin.domain.{ Plugin, PluginDescriptor }
import pl.mkubala.xml.NodeAttributesReader
import pl.mkubala.MessBuster.CanInject
import pl.mkubala.messBuster.domain.model.hook.ModelHook
import pl.mkubala.messBuster.domain.DocUnit
import pl.mkubala.messBuster.domain.model.field.Field
import pl.mkubala.messBuster.parser.ConcreteParser

case class Model(
  pluginIdentifier: String,
  name: String,
  fields: List[Field] = Nil,
  hooks: List[ModelHook] = Nil,
  attributes: ModelAttributes,
  constantClass: ModelConstants)
    extends DocUnit {

  type Selector = ModelIdentifier

  def sel = ModelIdentifier(pluginIdentifier, name)

  def withField(field: Field) = copy(fields = field :: fields)

  def withHook(hook: ModelHook) = copy(hooks = hook :: hooks)
}

case class ModelIdentifier(pluginIdentifier: String, modelName: String)

case class ModelConstants(generatedSource: String)

object QcadooModelParser {

  import collection.immutable.Seq

  def buildModels(plugin: PluginDescriptor): Seq[Model] =
    (plugin.xml \ "modules" \ "model") map (buildModel(plugin, _))

  def buildModel(plugin: PluginDescriptor, modelElement: Node) = {
    val modelPath = plugin.resourcesPath + '/' + plugin.identifier + '/' + ((modelElement \ "@resource").text)
    Model(plugin, trim(loadFile(modelPath)))
  }
}

object Model {

  implicit val canInjectModelToPlugin = new CanInject[Plugin, Model] {
    override val f = (plugin: Plugin, model: Model) => plugin.copy(models = plugin.models + (model.name -> model))
  }

  implicit val modelParser = ConcreteParser[Model](QcadooModelParser.buildModels _)

  def apply(plugin: PluginDescriptor, modelXmlRoot: Node): Model = {
    val modelName = (modelXmlRoot \ "@name").text
    val hooks = ((modelXmlRoot \ "hooks") flatMap (_.child map (ModelHook(_)))).toList
    val fields = ((modelXmlRoot \ "fields") flatMap (_.child map (Field(_)))).toList
    val modelConstants = ModelConstantsBuilder.build(modelName, fields)

    Model(plugin.identifier, modelName, fields, hooks, ModelAttributes(modelXmlRoot), modelConstants)
  }
}

object ModelConstantsBuilder {

  def build(modelName: String, fields: Seq[Field]): ModelConstants = {
    val className: String = modelName.head.toUpper + modelName.tail + "Fields"
    val sortedFieldNames = fields.map(_.name).sorted
    val sortedConstantsNames = sortedFieldNames.map(_.foldLeft("")((acc, ch) => {
      if (ch.isUpper) {
        acc + '_' + ch
      } else {
        acc + ch.toUpper
      }
    }))
    val prolog: String = s"""public final class $className {""" + "\n\n" +
      s"""    private $className() {}""" + '\n'

    val generatedSource = sortedConstantsNames.zip(sortedFieldNames).foldLeft(prolog)(
      (acc, f) => {
        val (constantName, fieldName) = f
        acc + '\n' + s"""    public static final String $constantName = "$fieldName";""" + '\n'
      }) + "\n}\n"
    ModelConstants(generatedSource)
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
