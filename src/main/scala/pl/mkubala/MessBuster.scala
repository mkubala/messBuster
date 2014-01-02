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

    val srcDirsOrFailure: Try[List[File]] = (dirsToScan map {
      path =>
        Try {
          new File(toAbsolutePath(path))
        }
    }).foldLeft(Try(List.empty[File])) {
      (dirsSeqOrFailure, dirOrFailure) =>
        for {
          dirs <- dirsSeqOrFailure
          dir <- dirOrFailure
        } yield dir :: dirs
    }

    val descriptorFilesOrFailure: Try[List[File]] = srcDirsOrFailure map {
      srcDirsSeq =>
        srcDirsSeq.flatMap(findRecursive)
    }

    val pluginDescriptorsOrFailure: Try[List[PluginDescriptor]] = descriptorFilesOrFailure flatMap {
      descriptorFilesSeq =>
        val filesAndDescriptors = descriptorFilesSeq.zip(descriptorFilesSeq.map(PluginDescriptor.apply))
        filesAndDescriptors.foldLeft(Try(List.empty[PluginDescriptor])) {
          (descriptorsSeqOrFailure, fileAndDescriptorOrFailurePair) =>
            for {
              descriptorsSeq <- descriptorsSeqOrFailure
            } yield {
              val (file, descriptorOrFailure) = fileAndDescriptorOrFailurePair
              descriptorOrFailure match {
                case Failure(cause) =>
                  logger.error(s"Can't parse ${file.getAbsolutePath}", cause)
                  descriptorsSeq
                case Success(descriptor) =>
                  descriptor :: descriptorsSeq
              }
            }
        }
    }

    val pluginsHolder = new PluginsHolder with FieldsInjector with HooksInjector

    pluginDescriptorsOrFailure foreach {
      _ foreach {
        descriptor =>
          pluginsHolder.addPlugin(Plugin(descriptor))
      }
    }

    pluginDescriptorsOrFailure foreach {
      _ foreach {
        descriptor =>
          for {
            (model, field) <- ExternalFieldsParser.parseFields(descriptor)
          } pluginsHolder.injectField(model, field)

          for {
            (model, hook) <- ExternalHooksParser.parseHooks(descriptor)
          } pluginsHolder.injectHook(model, hook)
      }
    }

    pluginDescriptorsOrFailure flatMap {
      _ =>
        val outputPath = toAbsolutePath(outputDir)
        Try(new File(outputPath)).flatMap(dstDir =>
          copyAssetsTo(dstDir).flatMap {
            _ => {
              persist {
                serialize(pluginsHolder.getPlugins)
              }(dstDir)
            }
          }
        )
    } match {
      case Failure(cause) => logger.error("Can't build docs", cause)
      case Success(()) => logger.info(s"Docs successfully generated in $outputDir")
    }
  }

}

object Main extends App {
  Qmdt.fire()
}
