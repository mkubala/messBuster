package pl.mkubala

import java.io.{File, PrintWriter}
import scala.xml.{Node, Utility}
import pl.mkubala.qmdt.io.PluginDescriptorScanner
import pl.mkubala.qmdt.model.domain.{ModelHook, ModelIdentifier}
import pl.mkubala.qmdt.model.domain.field.Field
import pl.mkubala.qmdt.plugin.container.PluginsHolder
import pl.mkubala.qmdt.plugin.domain.{Plugin, PluginDescriptor}
import pl.mkubala.qmdt.model.domain.InjectorPlugin
import org.apache.commons.io.FileUtils

trait OutputSerializer {
  type OutputFormat

  def serialize(input: Map[String, Plugin]): OutputFormat
}

trait JsonSerializer extends OutputSerializer {

  override type OutputFormat = String

  import org.json4s.native.Serialization.writePretty
  import org.json4s.DefaultFormats

  implicit val format = DefaultFormats

  override def serialize(input: Map[String, Plugin]) = {
    val jsonStr = writePretty(input)
    println(jsonStr)
    """
      var QMDT = QMDT || {};
      QMDT.data = %s;
    """.format(jsonStr)
  }

}

trait FilePersister {

  def persist(path: String, data: String) {
    val directory = new File(path)
    if (directory.isDirectory) {
      val file = new File(path + "/data.js")
      val out = new PrintWriter(file, "UTF-8")
      try {
        out.println(data)
      } finally {
        out.close()
      }
    } else {
      println("no files found")
    }
  }
}

trait FieldsParser {

  def parseFields(plugin: PluginDescriptor): Seq[(ModelIdentifier, Field)]

}

object ExternalFieldsParser extends FieldsParser {

  override def parseFields(plugin: PluginDescriptor): Seq[(ModelIdentifier, Field)] = {
    val parser = parse(plugin)(_)
    (plugin.xml \ "modules" \ "model-field") map parser
  }

  private def parse(plugin: PluginDescriptor)(node: Node): (ModelIdentifier, Field) = {
    val trimmedNode = Utility.trim(node)
    val targetModelIdentifier = ModelIdentifier(trimmedNode \ "@plugin" text, trimmedNode \ "@model" text)
    if (trimmedNode.child.isEmpty) {
      throw new IllegalStateException("Error in %s: broken node found - \n\t%s".format(plugin.identifier, trimmedNode))
    } else {
      implicit val injector = InjectorPlugin(Some(plugin.identifier))
      val injectedField = Field(trimmedNode.child.head)
      println(plugin.identifier + " -> " + targetModelIdentifier + " = " + injectedField)
      (targetModelIdentifier, injectedField)
    }
  }

}

trait HooksParser {

  def parseHooks(plugin: PluginDescriptor): Seq[(ModelIdentifier, ModelHook)]

}

object ExternalHooksParser extends HooksParser {
  override def parseHooks(plugin: PluginDescriptor) = {
    val parser = parse(plugin)(_)
    (plugin.xml \ "modules" \ "model-hook") map parser
  }

  private def parse(plugin: PluginDescriptor)(node: Node): (ModelIdentifier, ModelHook) = {
    val trimmedNode = Utility.trim(node)
    val targetModelIdentifier = ModelIdentifier(trimmedNode \ "@plugin" text, trimmedNode \ "@model" text)
    if (trimmedNode.child.isEmpty) {
      throw new IllegalStateException("Error in %s: broken node found - \n\t%s".format(plugin.identifier, trimmedNode))
    } else {
      val injectedHook = ModelHook(trimmedNode.child.head, Some(plugin.identifier))
      (targetModelIdentifier, injectedHook)
    }
  }
}

class AppArguments(private val args: Array[String]) {

  require(!args.isEmpty)
  val searchDirs = args.foldLeft(Seq[File]())((files: Seq[File], path: String) => {
    val file = new File(path)
    if (file.isDirectory)
      files :+ file
    else
      files
  })
  val dstDir = new File("output")

}

class WorldBuilder(skelPath: String) {

  val skelDir = new File(this.getClass.getClassLoader.getResource(skelPath).toURI)
  require(skelDir.isDirectory)

  def build(json: String, destDir: File) {
    require(destDir.isDirectory)
    FileUtils.copyDirectory(skelDir, destDir)
    persistData(json, destDir)
  }

  private def persistData(json: String, destDir: File) {
    val file = new File(destDir.getAbsolutePath + "/data.js")
    val out = new PrintWriter(file, "UTF-8")
    try {
      out.println(json)
    } finally {
      out.close()
    }
  }

}

object Qmdt extends PluginDescriptorScanner with JsonSerializer with FilePersister {

  import pl.mkubala.qmdt.plugin.domain._

//  def main(args: Array[String]) {
//    val appArgs = new AppArguments(args)
//    fire(appArgs.searchDirs, appArgs.dstDir)
//  }

  def fire(srcDirs: Seq[File], dstDir: File) {
    val foundDescriptorFiles = srcDirs flatMap findRecursive
    val pluginDescriptors: Seq[PluginDescriptor] = foundDescriptorFiles map (PluginDescriptor(_)) withFilter (_.isDefined) map (_.get)

    val pluginsHolder = new PluginsHolder with FieldsInjector with HooksInjector

    pluginDescriptors foreach {
      (pluginDescriptor) =>
        pluginsHolder.addPlugin(Plugin(pluginDescriptor))
    }

    //    val models = (pluginDescriptors flatMap (QcadooModelParser.buildModels))
    //    models foreach (pluginsHolder.addModel(_))

    val injectedFields = pluginDescriptors flatMap ExternalFieldsParser.parseFields
    injectedFields foreach {
      (fieldExtension) => pluginsHolder.injectField(fieldExtension._1, fieldExtension._2)
    }

    (pluginDescriptors flatMap ExternalHooksParser.parseHooks) foreach {
      (hookExtension) => pluginsHolder.injectHook(hookExtension._1, hookExtension._2)
    }

    new WorldBuilder("./skel").build(serialize(pluginsHolder.getPlugins), dstDir)
  }
}
