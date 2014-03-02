package pl.mkubala

import java.io.File
import scala.xml.{Node, Utility}
import pl.mkubala.messBuster.io._
import pl.mkubala.messBuster.model.domain.{QcadooModelParser, ModelHook}
import pl.mkubala.messBuster.model.domain.field.Field
import pl.mkubala.messBuster.plugin.container.PluginsHolder
import pl.mkubala.messBuster.plugin.domain.{Plugin, PluginDescriptor}
import pl.mkubala.messBuster.cli.{ConfigParametersProvider, ParametersProvider}
import com.typesafe.scalalogging.slf4j.Logging
import scala.util.Try
import pl.mkubala.messBuster.model.domain.InjectorPlugin
import scala.util.Failure
import scala.Some
import scala.util.Success
import pl.mkubala.messBuster.model.domain.ModelIdentifier

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
    """var QMDT = QMDT || {}; QMDT.data = %s;""".stripMargin.format(jsonStr)
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
  this: ParametersProvider with Persister[File] with AssetsManager with ParametersProvider =>
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
    val srcDirs: List[File] = (dirsToScan map {
      path =>
        (path, Try(new File(toAbsolutePath(path))))
    }).foldLeft(List.empty[File]) {
      (acc, t) =>
        t match {
          case (_, Success(file)) if file.exists() => file :: acc
          case (path, Success(file)) => {
            logger.warn(s"Directory $path doesn't exist - excluding from further processing")
            acc
          }
          case (path, Failure(cause)) => {
            logger.error(s"Can't read $path", cause)
            acc
          }
        }
    }

    logger.debug(s"srcDirs = $srcDirs")
    val foundDescriptorFiles = srcDirs.flatMap(findRecursive)
    logger.debug(s"descriptor files: ${foundDescriptorFiles}")

    val pluginDescriptors: Seq[PluginDescriptor] = foundDescriptorFiles.foldLeft(Vector.empty[PluginDescriptor]) {
      (acc, file) =>
            PluginDescriptor.apply(file) match {
              case Success(plugin) => acc :+ plugin
              case Failure(cause) => acc
            }
        }
    val pluginsHolder = new PluginsHolder with FieldsInjector with HooksInjector

    logger.debug(pluginDescriptors.mkString(", "))
    pluginDescriptors.foreach(descriptor => pluginsHolder.addPlugin(Plugin(descriptor)))

    val models = (pluginDescriptors flatMap (QcadooModelParser.buildModels))
    models foreach (pluginsHolder.addModel(_))

    val injectedFields: Seq[(ModelIdentifier, Field)] = pluginDescriptors flatMap ExternalFieldsParser.parseFields
    for {
      (model, field) <- injectedFields
    } pluginsHolder.injectField(model, field)

    for {
      descriptor <- pluginDescriptors
      (model, hook) <- ExternalHooksParser.parseHooks(descriptor)
    } pluginsHolder.injectHook(model, hook)

    val dstDir = new File(toAbsolutePath(outputDir))
      copyAssetsTo(dstDir) match {
        case Failure(cause) => logger.error("Can't copy assets", cause)
        case Success(_) => persist {
          logger.debug(s"Result map: \n${pluginsHolder.getPlugins}")
          serialize(pluginsHolder.getPlugins)
        }(dstDir) match {
          case Failure(cause) => logger.error("Can't serialize/write data", cause)
          case Success(_) => logger.info(s"Docs successfully generated in $outputDir")
        }
      }
  }
}

object Main extends App {
  Qmdt.fire()
}
