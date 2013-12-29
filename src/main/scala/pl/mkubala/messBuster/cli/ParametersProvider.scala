package pl.mkubala.messBuster.cli

import com.typesafe.config.ConfigFactory
import collection.immutable.Seq

trait ParametersProvider {

  def getProperty(key: String): String

  def outputDir: String

  def dirsToScan: Seq[String]

}

trait ConfigParametersProvider extends ParametersProvider {

  import ConfigParametersProvider._
  import collection.convert.WrapAsScala.asScalaIterator

  private lazy val config = ConfigFactory.load()

  private lazy val messBusterConfig = config.getConfig(configNamespace)

  def getProperty(key: String) = config.getString(key)

  def outputDir: String = messBusterConfig.getString(outputDirKey)

  def dirsToScan: Seq[String] =
    asScalaIterator(messBusterConfig.getStringList(dirsToScanKey).iterator()).toVector

}

object ConfigParametersProvider {
  val configNamespace = "messBuster"
  val outputDirKey = "outputDir"
  val dirsToScanKey = "dirsToScan"
}
