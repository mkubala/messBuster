package pl.mkubala.messBuster.io

import pl.mkubala.messBuster.cli.ParametersProvider
import org.apache.commons.io.FilenameUtils

object PathsNormalizer {
  protected val StartsWithHomePath = """^~/(.*)""".r
  protected val RelativePath = """^([^~/]+/.*)""".r
  protected val AbsolutePath = """^(/.*)""".r
}

trait PathsNormalizer {
  this: ParametersProvider =>

  import PathsNormalizer._

  def toAbsolutePath(path: String): String = FilenameUtils.normalize {
    path.trim match {
      case AbsolutePath(p) => p
      case RelativePath(p) => FilenameUtils.concat(getProperty("user.dir"), p)
      case StartsWithHomePath(p) => FilenameUtils.concat(getProperty("user.home"), p)
    }
  }
}
