import sbt._
import sbt.Keys._

object MessBuster extends Build {

  lazy val messBuster = Project(
    id = "messBuster",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "messBuster",
      organization := "pl.mkubala",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.3",
      libraryDependencies := Seq(
        "org.json4s" %% "json4s-native" % "3.2.5",
        "org.specs2" %% "specs2" % "2.0" % "test",
        "commons-io" % "commons-io" % "2.4",
        "com.typesafe" % "config" % "1.0.2",
        "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
        "org.slf4j" % "slf4j-log4j12" % "1.7.5")
    ) ++ sbtassembly.Plugin.assemblySettings
  )

}
