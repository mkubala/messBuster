package pl.mkubala.messBuster.parser

import pl.mkubala.messBuster.plugin.domain.PluginDescriptor
import scala.collection.immutable.Seq
import pl.mkubala.messBuster.domain.DocUnit

trait Parser[F, D] {
  val parse: F => Seq[D]
}

trait DescriptorParser[D] extends Parser[PluginDescriptor, D]

case class ConcreteParser[D](parse: PluginDescriptor => Seq[D]) extends DescriptorParser[D]
