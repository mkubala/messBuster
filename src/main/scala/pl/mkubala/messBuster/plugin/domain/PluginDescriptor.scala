package pl.mkubala.messBuster.plugin.domain

import scala.xml.Node
import scala.xml.XML
import java.io.File
import pl.mkubala.messBuster.plugin.container.PluginsHolder
import pl.mkubala.messBuster.model.domain._
import pl.mkubala.messBuster.model.domain.field.Field

case class PluginDescriptor(identifier: String, resourcesPath: String, xml: Node)

object PluginDescriptor {

  def apply(file: File): Option[PluginDescriptor] = {
    val resourcePath = file.getParentFile.getAbsolutePath
    try {
      val xml = XML.loadFile(file)

      val pluginName = (xml \ "@plugin").text
      Some(PluginDescriptor(pluginName, resourcePath, xml))
    } catch {
      case ex : Throwable => {
        println(file)
        ex.printStackTrace()
        None
      }
    }
  }

}

trait FieldsInjector {
  this: PluginsHolder =>

  def injectField(modelIdentifier: ModelIdentifier, field: Field) {
    implicit val injectorPlugin = InjectorPlugin(Some("test"))

    getModel(modelIdentifier) {
      (model) =>
        updateModel(model.withField(field))
    }(this.modelNotFoundAction)
  }
}

trait HooksInjector {
  this: PluginsHolder =>

  def injectHook(modelIdentifier: ModelIdentifier, hook: ModelHook) {
    getModel(modelIdentifier) {
      (model) =>
        updateModel(model.withHook(hook))
    }
  }
}
