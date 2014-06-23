package pl.mkubala.messBuster.model.domain

trait Injectable {
  val injectorPlugin: Option[String]
}

sealed trait Injector {
  def identifier: Option[String]
}

case class InjectorPlugin(identifier: Option[String]) extends Injector

object EmptyInjectorPlugin extends Injector {
  val identifier: Option[String] = None
}