package pl.mkubala.messBuster.serializer

import pl.mkubala.messBuster.plugin.domain.Plugin
import com.typesafe.scalalogging.slf4j.Logging

trait OutputSerializer {
  type OutputFormat

  def serialize(input: Map[String, Plugin]): OutputFormat
}

trait JsonSerializer extends OutputSerializer with Logging {

  override type OutputFormat = String

  import org.json4s.native.Serialization.writePretty
  import org.json4s.DefaultFormats

  implicit val format = DefaultFormats

  override def serialize(input: Map[String, Plugin]) = {
    val jsonStr = writePretty(input)
    logger.debug(s"Result JSON: \n$jsonStr")
    """var QMDT = QMDT || {}; QMDT.data = %s;""".format(jsonStr)
  }

}
