package pl.mkubala.io

import pl.mkubala.messBuster.cli.ParametersProvider
import scala.collection.immutable.Seq
import pl.mkubala.messBuster.io.PathsNormalizer
import org.specs2.mutable.Specification

class PathsNormalizerSpec extends Specification {

  trait ParameterProviderMock extends ParametersProvider {
    def getProperty(key: String): String = key match {
      case "user.home" => "/home/user/mb"
      case "user.dir" => "/usr/local/mb"
    }

    def outputDir: String = "out"

    def dirsToScan: Seq[String] = Seq("in1", "in2")
  }

  val pathsNormalizer = new PathsNormalizer with ParameterProviderMock

  "PathsNormalizer" should {

    """Normalize "~/some/dir"""" in {
      pathsNormalizer.toAbsolutePath("~/some/dir") must_== "/home/user/mb/some/dir"
    }

    """Normalize "some/dir"""" in {
      pathsNormalizer.toAbsolutePath("some/dir") must_== "/usr/local/mb/some/dir"
    }

    """Normalize "./some/dir"""" in {
      pathsNormalizer.toAbsolutePath("./some/dir") must_== "/usr/local/mb/some/dir"
    }

    """Normalize "../some/dir"""" in {
      pathsNormalizer.toAbsolutePath("../some/dir") must_== "/usr/local/some/dir"
    }

    """Left "/home/user/some/dir" untouched""" in {
      pathsNormalizer.toAbsolutePath("/home/user/some/dir") must_== "/home/user/some/dir"
    }

    """Left "/home/user/some path/dir" untouched""" in {
      pathsNormalizer.toAbsolutePath("/home/user/some path/dir") must_== "/home/user/some path/dir"
    }

    """Trim "  /home/user/some path/dir  """" in {
      pathsNormalizer.toAbsolutePath("  /home/user/some path/dir  ") must_== "/home/user/some path/dir"
    }

  }

}
