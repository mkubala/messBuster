import com.github.retronym.SbtOneJar
import sbt._
import sbt.Keys._

object QmdtBuild extends Build {

  lazy val qmdt = Project(
    id = "qmdt",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "qmdt",
      organization := "pl.mkubala",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.2",
      libraryDependencies := Seq(
        "org.json4s" %% "json4s-native" % "3.2.4-SNAPSHOT",
//        "org.specs2" % "specs2_2.9.2" % "1.12.3" % "test",
        "org.specs2" %% "specs2" % "2.0" % "test",
        "commons-io" % "commons-io" % "2.4",
        "org.scala-lang" % "scala-swing" % "2.10.2")
        //"org.scalafx" %% "scalafx" % "1.0.0-M4")
    ) ++ SbtOneJar.oneJarSettings
  )

}
