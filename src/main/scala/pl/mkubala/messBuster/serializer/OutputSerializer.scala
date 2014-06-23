package pl.mkubala.messBuster.serializer

import pl.mkubala.messBuster.plugin.domain.Plugin
import com.typesafe.scalalogging.slf4j.Logging
import pl.mkubala.messBuster.domain.model.ModelIdentifier

trait OutputSerializer {
  type OutputFormat

  def serialize(input: Map[String, Plugin], blindBtFields: Map[ModelIdentifier, String]): OutputFormat
}

trait JsonSerializer extends OutputSerializer with Logging {

  override type OutputFormat = String

  import org.json4s.jackson.Serialization.writePretty
  import org.json4s.DefaultFormats

  implicit val format = DefaultFormats

  override def serialize(input: Map[String, Plugin], blindBtFields: Map[ModelIdentifier, String]) = {
    val dataJson = writePretty(input)
    val blindBtFieldsJson = writePretty {
      blindBtFields.map { v =>
        val (ModelIdentifier(pi, mn), bt) = v
        (s"$pi.$mn", bt)
      }
    }
    s"""var QMDT = QMDT || {}; QMDT.data = $dataJson; QMDT.problems = QMDT.problems || {}; QMDT.problems.blindBtFields = $blindBtFieldsJson;"""
  }

}
