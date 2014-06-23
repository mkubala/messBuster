package pl.mkubala

import java.io.File
import pl.mkubala.messBuster.io._
import pl.mkubala.messBuster.cli.{ ParametersAware, ConfigParametersAware }
import com.typesafe.scalalogging.slf4j.Logging
import scala.util.Try
import pl.mkubala.messBuster.parser.{ ParseResult, Parser }
import pl.mkubala.messBuster.serializer.{ OutputSerializer, JsonSerializer }
import scala.collection.immutable.Seq
import scala.util.Failure
import scala.util.Success
import pl.mkubala.messBuster.domain.Container
import pl.mkubala.messBuster.domain.model.field.{ CollectionFieldType, Field, BelongsToFieldType }
import pl.mkubala.messBuster.domain.model.hook.ModelHook
import pl.mkubala.messBuster.domain.model.{ Model, ModelIdentifier }
import pl.mkubala.messBuster.parser.ParseResult.ParseResult
import pl.mkubala.messBuster.parser.ParseResult

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
        val plugins: Container[Plugin] = buildPluginsInfo(getSrcDirs(parameters.dirsToScan), outDir)
        lazy val models = for {
          (pluginIdentifier, plugin) <- plugins.values
          (modelName, model) <- plugin.models
        } yield (ModelIdentifier(pluginIdentifier, modelName), model)
        lazy val blindBelongsToFields = findBlindBelongsToFields(models.toMap).toMap
        buildDocs(plugins, blindBelongsToFields, outDir)
      }
    }
  }

  private def getSrcDirs(dirsToScan: Seq[String]): List[File] = (dirsToScan map {
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

  trait CanInject[D, E] {
    val f: (D, E) => D
  }

  private def buildPluginsInfo(srcDirs: Seq[File], outDir: File): Container[Plugin] = {
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

    val plugins: Container[Plugin] = Container.from[Plugin](pluginDescriptors)

    val modelsWithInjections: Container[Model] = buildModels(pluginDescriptors)

    val effectiveModels =
      modelsWithInjections.values.toList map { modelEntry =>
        val (modelId, model) = modelEntry
        ParseResult.success((modelId.pluginIdentifier, model))
      }

    plugins inject effectiveModels
  }

  private def buildModels(pluginDescriptors: Seq[PluginDescriptor]): Container[Model] = {
    val models = Container.from[Model](pluginDescriptors)

    val injectedFields: Seq[ParseResult[(Model#Selector, Field)]] =
      pluginDescriptors.flatMap(pd => ParseResult.from[PluginDescriptor, (Model#Selector, Field)](pd))

    val injectedHooks: Seq[ParseResult[(Model#Selector, ModelHook)]] =
      pluginDescriptors.flatMap(pd => ParseResult.from[PluginDescriptor, (Model#Selector, ModelHook)](pd))

    models inject injectedHooks inject injectedFields
  }

  private def findBlindBelongsToFields(models: Map[ModelIdentifier, Model]): Seq[(ModelIdentifier, String)] = {

    def findRelatedModel(btType: BelongsToFieldType): Option[Model] =
      models.get(btType.relatedModel)

    def hasCorrespondingField(model: Model, btType: BelongsToFieldType, btModelId: ModelIdentifier) = {
      val btName = btType.name
      (model.fields find {
        _.fieldType match {
          case CollectionFieldType(_, _, relatedModel, joinFieldName, _) if (joinFieldName == btName && relatedModel == btModelId) => true
          case _ => false
        }
      }).isDefined
    }

    val belongsToFields: Seq[(ModelIdentifier, BelongsToFieldType, String)] = (models flatMap { v =>
      val (_, model) = v
      lazy val modelIdentifier = ModelIdentifier(model.pluginIdentifier, model.name)

      model.fields collect {
        case Field(fieldType: BelongsToFieldType, name, _, _, _) =>
          (modelIdentifier, fieldType, name)
      }
    }).toList

    for {
      (btModelId, btType, btName) <- belongsToFields
      model <- findRelatedModel(btType)
      if !hasCorrespondingField(model, btType, btModelId)
    } yield (btModelId, btName)
  }

  private def buildDocs(plugins: => Container[Plugin], blindBtFields: => Map[ModelIdentifier, String], outDir: File) {
    copyAssetsTo(outDir) match {
      case Failure(cause) => logger.error("Can't copy assets", cause)
      case Success(_) => persist {
        serialize(plugins.values, blindBtFields)
      }(outDir) match {
        case Failure(cause) => logger.error("Can't serialize/write data", cause)
        case Success(_) => logger.info(s"Docs successfully generated in '$outDir'")
      }
    }
  }

}
