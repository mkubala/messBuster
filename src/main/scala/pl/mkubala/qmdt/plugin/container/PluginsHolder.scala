package pl.mkubala.qmdt.plugin.container

import pl.mkubala.qmdt.model.domain.ModelIdentifier
import pl.mkubala.qmdt.plugin.domain.Plugin
import pl.mkubala.qmdt.model.domain.Model

class PluginsHolder {

  protected[this] var plugins = Map[String, Plugin]()

  implicit def pluginNotFoundAction = (pluginId: String) =>
    println("-- plugin %s does not exist in current scope - OMMITTING".format(pluginId))

  implicit def modelNotFoundAction = (modelIdentifier: ModelIdentifier) =>
    println("-- model %s.%s does not exist in current scope - OMMITTING".format(modelIdentifier.pluginIdentifier, modelIdentifier.modelName))

  protected def getPlugin(pluginIdentifier: String)(existAction: Plugin => Unit)(implicit notExistAction: String => Unit) {
    plugins.get(pluginIdentifier) match {
      case Some(plugin) => existAction(plugin)
      case None => notExistAction(pluginIdentifier)
    }
  }

  protected[plugin] def getModel(modelIdentifier: ModelIdentifier)(existAction: Model => Unit)(implicit notExistAction: ModelIdentifier => Unit) {
    getPlugin(modelIdentifier.pluginIdentifier) { plugin =>
      plugin.getModel(modelIdentifier.modelName)(existAction)(modelName => notExistAction(modelIdentifier))
    }(pluginIdentifier => notExistAction(modelIdentifier))
  }

  def addPlugin(plugin: Plugin) {
    plugins += (plugin.identifier -> plugin)
  }

  def updateModel(model: Model) = addModel(model)

  def addModel(model: Model) {
    getPlugin(model.pluginIdentifier) { plugin =>
      plugins += (model.pluginIdentifier -> plugin.withModel(model))
    } { _ =>
      println("-- plugin %s not found".format(model.pluginIdentifier))
    }
  }

  def getPlugins = plugins

}