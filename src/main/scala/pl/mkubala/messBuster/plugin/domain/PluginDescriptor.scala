package pl.mkubala.messBuster.plugin.domain

import scala.xml.Node
import scala.xml.XML
import java.io.File
import pl.mkubala.messBuster.plugin.container.PluginsHolder
import pl.mkubala.messBuster.model.domain._
import pl.mkubala.messBuster.model.domain.field.Field
import scala.util.Try
import com.typesafe.scalalogging.slf4j.Logging

case class PluginDescriptor(identifier: String, resourcesPath: String, xml: Node)

object PluginDescriptor extends Logging {

  def apply(file: File): Try[PluginDescriptor] = {
    val resourcePath = file.getParentFile.getAbsolutePath
    Try {
      val xml = XML.loadFile(file)
      val pluginName = (xml \ "@plugin").text
      PluginDescriptor(pluginName, resourcePath, xml)
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
