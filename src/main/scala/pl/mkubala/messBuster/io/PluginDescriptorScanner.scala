package pl.mkubala.messBuster.io

import java.io.File

import scala.Array.canBuildFrom

trait PluginDescriptorScanner {

  def findRecursive(directory: File): List[File] =
    if (directory.isDirectory) {
      val subDirsPlugins = (directory.listFiles withFilter (_.isDirectory) flatMap findRecursive).toList
      tryLoadPluginDescriptor(directory) match {
        case Some(pluginDesc) => pluginDesc :: subDirsPlugins
        case None => subDirsPlugins
      }
    } else {
      List()
    }

  private def tryLoadPluginDescriptor(dir: File): Option[File] = {
    val pluginDescriptor = new File(dir.getAbsolutePath + "/src/main/resources/qcadoo-plugin.xml")
    if (pluginDescriptor.exists) {
      Some(pluginDescriptor)
    } else {
      None
    }
  }
}