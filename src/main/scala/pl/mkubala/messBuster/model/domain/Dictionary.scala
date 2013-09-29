package pl.mkubala.messBuster.model.domain

import pl.mkubala.messBuster.plugin.domain.PluginDescriptor

case class Dictionary(name: String, items: List[DictionaryItem] = Nil) {
  def withItem(item: DictionaryItem) = {
    Dictionary(name, item :: items)
  }
}

case class DictionaryItem(value: String, injectorPlugin: Option[String] = None) extends Injectable

object Dictionary {

  def apply(descriptor: PluginDescriptor): List[Dictionary] =
    ((descriptor.xml \ "modules" \ "dictionary") foldLeft List[Dictionary]()) { (dictionariesList, dictionaryNode) =>
      dictionaryNode.attribute("name") match {
        case Some(name) => Dictionary(name.text) :: dictionariesList
        case None => dictionariesList
      }
    }

}
