package pl.mkubala.messBuster.parser

import pl.mkubala.messBuster.plugin.domain.PluginDescriptor
import pl.mkubala.messBuster.model.domain.{InjectorPlugin, ModelIdentifier}
import pl.mkubala.messBuster.model.domain.field.Field
import scala.xml.{Utility, Node}
import com.typesafe.scalalogging.slf4j.Logging
import scala.util.{Try, Failure, Success}

trait FieldsParser {

  def parseFields(plugin: PluginDescriptor): Seq[Try[(ModelIdentifier, Field)]]

}

object ExternalFieldsParser extends FieldsParser with Logging {

  override def parseFields(plugin: PluginDescriptor): Seq[Try[(ModelIdentifier, Field)]] = {
    val parser = parse(plugin)(_)
    (plugin.xml \ "modules" \ "model-field") map parser
  }

  private def parse(plugin: PluginDescriptor)(node: Node): Try[(ModelIdentifier, Field)] = {
    val trimmedNode = Utility.trim(node)
    val targetModelIdentifier = ModelIdentifier(trimmedNode \ "@plugin" text, trimmedNode \ "@model" text)
    if (trimmedNode.child.isEmpty) {
      Failure(new IllegalStateException(s"Error in ${plugin.identifier}: broken node found - \n\t$trimmedNode"))
    } else {
      implicit val injector = InjectorPlugin(Some(plugin.identifier))
      val injectedField = Field(trimmedNode.child.head)
      logger.debug(s"${plugin.identifier} -> $targetModelIdentifier = $injectedField")
      Success((targetModelIdentifier, injectedField))
    }
  }

}