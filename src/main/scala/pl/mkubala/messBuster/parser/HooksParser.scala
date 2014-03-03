package pl.mkubala.messBuster.parser

import pl.mkubala.messBuster.plugin.domain.PluginDescriptor
import pl.mkubala.messBuster.model.domain.{ModelHook, ModelIdentifier}
import scala.xml.{Utility, Node}
import scala.util.{Success, Failure, Try}

trait HooksParser {

  def parseHooks(plugin: PluginDescriptor): Seq[Try[(ModelIdentifier, ModelHook)]]

}

object ExternalHooksParser extends HooksParser {
  override def parseHooks(plugin: PluginDescriptor): Seq[Try[(ModelIdentifier, ModelHook)]] = {
    val parser = parse(plugin)(_)
    (plugin.xml \ "modules" \ "model-hook") map parser
  }

  private def parse(plugin: PluginDescriptor)(node: Node): Try[(ModelIdentifier, ModelHook)] = {
    val trimmedNode = Utility.trim(node)
    val targetModelIdentifier = ModelIdentifier(trimmedNode \ "@plugin" text, trimmedNode \ "@model" text)
    if (trimmedNode.child.isEmpty) {
      Failure(new IllegalStateException("Error in %s: broken node found - \n\t%s".format(plugin.identifier, trimmedNode)))
    } else {
      val injectedHook = ModelHook(trimmedNode.child.head, Some(plugin.identifier))
      Success((targetModelIdentifier, injectedHook))
    }
  }
}