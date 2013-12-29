package pl.mkubala

import java.io.File
import scala.xml.{Node, Utility}
import pl.mkubala.messBuster.io._
import pl.mkubala.messBuster.model.domain.ModelHook
import pl.mkubala.messBuster.model.domain.field.Field
import pl.mkubala.messBuster.plugin.container.PluginsHolder
import pl.mkubala.messBuster.plugin.domain.{Plugin, PluginDescriptor}
import pl.mkubala.messBuster.cli.{ConfigParametersProvider, ParametersProvider}
import com.typesafe.scalalogging.slf4j.Logging
import pl.mkubala.messBuster.model.domain.InjectorPlugin
import scala.util.{Try, Success, Failure}
import scala.Some
import pl.mkubala.messBuster.model.domain.ModelIdentifier

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

trait Qmdt {
  this: ParametersProvider with Persister with AssetsManager with ParametersProvider =>
}

object Qmdt extends Qmdt
with PluginDescriptorScanner
with JsonSerializer
with FilePersister
with ConfigParametersProvider
with JarAssetsManager
with PathsNormalizer
with Logging {

  import pl.mkubala.messBuster.plugin.domain._

  val assetsRootPath: String = "skel/"

  def fire() {
    val srcDirs = dirsToScan map {
      path =>
        new File(toAbsolutePath(path))
    }

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

    Try(new File(toAbsolutePath(outputDir))).flatMap(dstDir =>
      copyAssetsTo(dstDir).flatMap {
        _ => {
          persist {
            serialize(pluginsHolder.getPlugins)
          }(dstDir)
        }
      }
    ) match {
      case Failure(cause) => logger.error("Can't build docs", cause)
      case Success(()) => logger.info(s"Docs successfully generated in $outputDir")
    }
  }
}

object Main extends App {
  Qmdt.fire()
}
