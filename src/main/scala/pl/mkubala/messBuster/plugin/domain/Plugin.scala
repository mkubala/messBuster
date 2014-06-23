package pl.mkubala.messBuster.plugin.domain

import pl.mkubala.messBuster.model.domain.Dictionary
import pl.mkubala.messBuster.domain.DocUnit
import pl.mkubala.messBuster.parser.ConcreteParser
import pl.mkubala.messBuster.domain.model.Model

case class Plugin(identifier: String,
    pluginInfo: PluginInfo,
    models: Map[String, Model] = Map(),
    dictionaries: List[Dictionary] = Nil) extends DocUnit {

  type Selector = String

  def sel = identifier

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

  implicit val pluginParser = ConcreteParser { descriptor =>
    Plugin(descriptor.identifier, PluginInfo(descriptor), Map(), Dictionary(descriptor)) :: Nil
  }

}