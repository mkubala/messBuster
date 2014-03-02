package pl.mkubala.messBuster.io

import scala.util.Try
import java.io.{PrintWriter, File}

trait Persister[T] {

  def persist(data: => String)(targetRoot: T): Try[Unit]

}

trait FilePersister extends Persister[File] {

  def persist(data: => String)(targetRoot: File): Try[Unit] = {
      Try {
        require(targetRoot.exists() && targetRoot.isDirectory,
          s"Directory (${targetRoot.getAbsolutePath}) have to be a directory.")
        val file = new File(targetRoot, "/data.js")
        val out = new PrintWriter(file, "UTF-8")
        try {
          out.println(data)
        } finally {
          out.close()
        }
      }
    }
}