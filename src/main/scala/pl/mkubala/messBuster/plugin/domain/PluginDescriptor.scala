package pl.mkubala.messBuster.plugin.domain

import scala.xml.Node
import scala.xml.XML
import java.io.File
import scala.util.Try
import com.typesafe.scalalogging.slf4j.Logging

case class PluginDescriptor(identifier: String, resourcesPath: String, xml: Node)

object PluginDescriptor extends Logging {

  def apply(file: File): Try[PluginDescriptor] = {
    val resourcePath = file.getParentFile.getAbsolutePath
    Try {
      val xml = XML.loadFile(file)
      val pluginName = (xml \ "@plugin").text
      PluginDescriptor(pluginName, resourcePath, xml)
    }
  }

}
