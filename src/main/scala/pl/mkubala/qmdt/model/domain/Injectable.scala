package pl.mkubala.qmdt.model.domain

trait Injectable {
  val injectorPlugin: Option[String]
}

trait Injector {
  def identifier: Option[String]
}
case class InjectorPlugin(identifier: Option[String]) extends Injector
object EmptyInjectorPlugin extends Injector {
  val identifier: Option[String] = None
}