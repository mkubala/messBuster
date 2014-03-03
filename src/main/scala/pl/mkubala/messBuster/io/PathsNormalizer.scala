package pl.mkubala.messBuster.io

import pl.mkubala.messBuster.cli.ParametersAware
import org.apache.commons.io.FilenameUtils

object PathsNormalizer {
  protected val StartsWithHomePath = """^~/(.*)""".r
  protected val RelativePath = """^([^~/]+/.*)""".r
  protected val AbsolutePath = """^(/.*)""".r
}

trait PathsNormalizer {
  this: ParametersAware =>

  import PathsNormalizer._

  def toAbsolutePath(path: String): String = FilenameUtils.normalize {
    path.trim match {
      case AbsolutePath(p) => p
      case RelativePath(p) => FilenameUtils.concat(parameters.getProperty("user.dir"), p)
      case StartsWithHomePath(p) => FilenameUtils.concat(parameters.getProperty("user.home"), p)
    }
  }
}
