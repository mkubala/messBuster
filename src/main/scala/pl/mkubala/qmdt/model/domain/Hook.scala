package pl.mkubala.qmdt.model.domain

import scala.xml.Node
import scala.xml.Utility.trim

trait Hook {
  val hookClass: String
  val hookMethod: String
}

case class ModelHook(hookType: String, hookClass: String, hookMethod: String, injectorPlugin: Option[String]) extends Hook with Injectable

object ModelHook {

  def apply(hookNode: Node, injectedBy: Option[String] = None): ModelHook = {
    val trimmedHookNode: Node = trim(hookNode)
    ModelHook(trimmedHookNode.label, (trimmedHookNode \ "@class").text, (trimmedHookNode \ "@method").text, injectedBy)
  }

}
