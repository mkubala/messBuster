package pl.mkubala.messBuster.io

import java.io.File
import scala.util.Try
import com.typesafe.scalalogging.slf4j.Logging
import java.util.jar.JarFile
import scala.collection.convert.Wrappers.JEnumerationWrapper
import org.apache.commons.io.FileUtils
import pl.mkubala.messBuster.cli.ParametersProvider
import pl.mkubala.Main


trait AssetsManager {

  val assetsRootPath: String

  def copyAssetsTo(target: File): Try[Unit]

}

trait JarAssetsManager extends AssetsManager with Logging {
  this: ParametersProvider =>

  def copyAssetsTo(targetRoot: File): Try[Unit] = {
    require(targetRoot.canWrite, s"Can't write to ${targetRoot.getAbsolutePath}")
    require(targetRoot.isDirectory || targetRoot.mkdir(), s"Can't create output directory (${targetRoot.getAbsolutePath})")
    val assetsRootPathLength = assetsRootPath.length
    getAssetPaths(assetsRootPath).flatMap(_.foldLeft(Try()) {
      (res: Try[Unit], path: String) =>
        res.flatMap(_ => Try {
          val target = new File(targetRoot.getAbsolutePath + '/' + path.drop(assetsRootPathLength))
          val resourceInputStream = getClass.getClassLoader.getResourceAsStream(path)
          println(s"$path = $resourceInputStream -> $target")
          FileUtils.copyInputStreamToFile(resourceInputStream, target)
        })
    })
  }

  private def getAssetPaths(assetsRootPath: String): Try[Set[String]] = Try {
    val jarPath = Main.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
    val jar = new JarFile(jarPath)
    JEnumerationWrapper(jar.entries()).withFilter(entry =>
        !entry.isDirectory && entry.getName.startsWith(assetsRootPath)
      ).map(_.getName).toSet
  }

}