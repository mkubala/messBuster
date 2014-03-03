package pl.mkubala

import java.io.File
import pl.mkubala.messBuster.io._
import pl.mkubala.messBuster.model.domain.field.Field
import pl.mkubala.messBuster.plugin.container.PluginsHolder
import pl.mkubala.messBuster.cli.{ParametersAware, ConfigParametersAware}
import com.typesafe.scalalogging.slf4j.Logging
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import pl.mkubala.messBuster.model.domain.{ModelHook, QcadooModelParser, ModelIdentifier}
import pl.mkubala.messBuster.parser.{ExternalFieldsParser, ExternalHooksParser}
import pl.mkubala.messBuster.serializer.{OutputSerializer, JsonSerializer}

object Main extends App {
  MessBuster.fire()
}

trait MessBuster {
  this: ParametersAware with Persister[File] with AssetsManager with PathsNormalizer with OutputSerializer =>

  def fire(): Unit

}

object MessBuster extends MessBuster
            with PluginDescriptorScanner
            with JsonSerializer
            with FilePersister
            with ConfigParametersAware
            with JarAssetsManager
            with PathsNormalizer
            with Logging {

  import pl.mkubala.messBuster.plugin.domain._

  def fire() {
    val outputDir: Either[File, String] = getOutputDir(parameters.outputDir)
    outputDir match {
      case Right(failureMsg) => logger.error(failureMsg)
      case Left(outDir) => {
        lazy val pluginsHolder: PluginsHolder = buildPluginsInfo(getSrcDirs(parameters.dirsToScan), outDir)
        buildDocs(pluginsHolder, outDir)
      }
    }
  }

  private def getSrcDirs(dirsToScan: Seq[String]):List[File] = (dirsToScan map {
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

  private def getOutputDir(path: String): Either[File, String] = {
    val outputPath = toAbsolutePath(path)
    val dstDir = new File(outputPath)
    if (!dstDir.exists()) {
      Right(s"Output directory '$outputPath' doesn't exist. Aborting.")
    } else if (!dstDir.isDirectory) {
      Right(s"Output directory '$outputPath' isn't a directory. Aborting.")
    } else if (!dstDir.canWrite) {
      Right(s"Can't write into '$outputPath'. Aborting.")
    } else {
      Left(dstDir)
    }
  }

  private def buildPluginsInfo(srcDirs: Seq[File], outDir: File): PluginsHolder = {
    val foundDescriptorFiles = srcDirs.flatMap(findRecursive)
    val pluginDescriptors: Seq[PluginDescriptor] = foundDescriptorFiles.foldLeft(Vector.empty[PluginDescriptor]) {
      (acc, file) =>
            PluginDescriptor.apply(file) match {
              case Success(plugin) => acc :+ plugin
              case Failure(cause) => {
                logger.warn(s"Can't build plugin descriptor from '${file.getAbsolutePath}', cause: ${cause.getMessage} - omitting");
                acc
              }
            }
        }
    val pluginsHolder = new PluginsHolder with FieldsInjector with HooksInjector

    pluginDescriptors.foreach(descriptor => pluginsHolder.addPlugin(Plugin(descriptor)))

    val models = (pluginDescriptors flatMap (QcadooModelParser.buildModels))
    models foreach (pluginsHolder.addModel(_))

    val injectedFields: Seq[Try[(ModelIdentifier, Field)]] = pluginDescriptors flatMap ExternalFieldsParser.parseFields
    injectedFields foreach {
      case Failure(cause) => logger.warn(cause.getMessage)
      case Success((model, field)) => pluginsHolder.injectField(model, field)
    }

    val injectedHooks: Seq[Try[(ModelIdentifier, ModelHook)]] = pluginDescriptors flatMap ExternalHooksParser.parseHooks
    injectedHooks foreach {
      case Failure(cause) => logger.warn(cause.getMessage)
      case Success((model, hook)) => pluginsHolder.injectHook(model, hook)
    }

    pluginsHolder
  }

  private def buildDocs(pluginsHolder: => PluginsHolder, outDir: File) {
    copyAssetsTo(outDir) match {
      case Failure(cause) => logger.error("Can't copy assets", cause)
      case Success(_) => persist {
        serialize(pluginsHolder.getPlugins)
      }(outDir) match {
        case Failure(cause) => logger.error("Can't serialize/write data", cause)
        case Success(_) => logger.info(s"Docs successfully generated in '$outDir'")
      }
    }
  }

}

