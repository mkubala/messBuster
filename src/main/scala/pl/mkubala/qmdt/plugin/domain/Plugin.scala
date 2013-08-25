package pl.mkubala.qmdt.plugin.domain

import pl.mkubala.qmdt.model.domain.QcadooModelParser
import pl.mkubala.qmdt.model.domain.Model
import pl.mkubala.qmdt.model.domain.Dictionary
import pl.mkubala.qmdt.model.domain.Dictionary
import pl.mkubala.qmdt.model.domain.DictionaryItem

case class Plugin(identifier: String, pluginInfo: PluginInfo, models: Map[String, Model] = Map(), dictionaries: List[Dictionary] = Nil) {

  private def copy(models: Map[String, Model] = models, dictionaries: List[Dictionary] = dictionaries) = {
    Plugin(identifier, pluginInfo, models, dictionaries)
  }

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