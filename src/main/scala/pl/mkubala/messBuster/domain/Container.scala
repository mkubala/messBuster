package pl.mkubala.messBuster.domain

import scala.collection.immutable.Seq
import pl.mkubala.messBuster.plugin.domain.PluginDescriptor
import pl.mkubala.messBuster.parser.Parser
import pl.mkubala.MessBuster.CanInject
import pl.mkubala.messBuster.parser.ParseResult.ParseResult

case class Container[D <: DocUnit](values: Map[D#Selector, D]) {

  def get(selector: D#Selector): Option[D] = values.get(selector)

  def +(result: D): Container[D] = copy(values + (result.sel -> result))

  def inject[E](injections: Seq[ParseResult[(D#Selector, E)]])(implicit c: CanInject[D, E]): Container[D] =
    (injections :\ this) { (injection, scanResults) =>
      injection match {
        case Right((selector, elem)) =>
          scanResults.get(selector) match {
            case Some(entry) => scanResults + (c.f(entry, elem))
            case None => scanResults
          }
        case _ => scanResults
      }
    }
}

object Container {
  def empty[D <: DocUnit]: Container[D] = Container(Map())

  def from[D <: DocUnit](pds: Seq[PluginDescriptor])(implicit p: Parser[PluginDescriptor, D]): Container[D] =
    (Container.empty[D] /: pds.flatMap(p.parse(_)))(_ + _)

}