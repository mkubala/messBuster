package pl.mkubala.messBuster.plugin.domain

import pl.mkubala.messBuster.model.domain.QcadooModelParser
import pl.mkubala.messBuster.model.domain.Model
import pl.mkubala.messBuster.model.domain.Dictionary
import pl.mkubala.messBuster.model.domain.Dictionary
import pl.mkubala.messBuster.model.domain.DictionaryItem

case class Plugin(identifier: String, pluginInfo: PluginInfo, models: Map[String, Model] = Map(), dictionaries: List[Dictionary] = Nil) {

  def withModel(model: Model) =
    copy(models = models + (model.name -> model))

  def withDictionary(dictionary: Dictionary) =
    copy(dictionaries = dictionary :: dictionaries)

  implicit def defaultNotExistAction =
    (pluginId: String) => println("-- model %s does not exist in current scope - OMMITTING".format(pluginId))

  protected[plugin] def getModel(modelIdentifier: String)(existAction: Model => Unit)(implicit notExistAction: String => Unit) {
    models.get(modelIdentifier) match {
      case Some(model) => existAction(model)
      case None => notExistAction(modelIdentifier)
    }
  }

  override def toString = models.toString
}

object Plugin {

  def apply(descriptor: PluginDescriptor): Plugin = {
    val modelsMap = QcadooModelParser.buildModels(descriptor) map { model =>
      (model.name -> model)
    }
    Plugin(descriptor.identifier, PluginInfo(descriptor), modelsMap.toMap, Dictionary(descriptor))
  }
}