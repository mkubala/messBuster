package pl.mkubala.messBuster.domain.model.hook

import scala.xml.Node
import scala.xml.Utility.trim
import pl.mkubala.MessBuster.CanInject
import pl.mkubala.messBuster.domain.model.Model
import pl.mkubala.messBuster.model.domain.Injectable
import pl.mkubala.messBuster.parser.{ ParseResult, ConcreteParser, ExternalHooksParser, Parser }
import pl.mkubala.messBuster.plugin.domain.PluginDescriptor
import pl.mkubala.messBuster.parser.ParseResult.ParseResult

trait Hook {
  val hookClass: String
  val hookMethod: String
}

case class ModelHook(hookType: String, hookClass: String, hookMethod: String, injectorPlugin: Option[String]) extends Hook with Injectable

object ModelHook {

  implicit val canInjectHookToModel = new CanInject[Model, ModelHook] {
    override val f = (model: Model, hook: ModelHook) => model.copy(hooks = hook :: model.hooks)
  }

  implicit val modelHookParser: Parser[PluginDescriptor, ParseResult[(Model#Selector, ModelHook)]] =
    ConcreteParser[ParseResult[(Model#Selector, ModelHook)]] { pd =>
      ParseResult.convert(ExternalHooksParser.parseHooks(pd))
    }

  def apply(hookNode: Node, injectedBy: Option[String] = None): ModelHook = {
    val trimmedHookNode: Node = trim(hookNode)
    ModelHook(trimmedHookNode.label, (trimmedHookNode \ "@class").text, (trimmedHookNode \ "@method").text, injectedBy)
  }

}
