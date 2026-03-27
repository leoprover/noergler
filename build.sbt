ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.18"

lazy val root = (project in file("."))
  .settings(
    name := "Noergler",
    organization := "org.leo",
    Compile/mainClass := Some("noergler.Noergler"),

    libraryDependencies += "io.github.leoprover" %% "scala-tptp-parser" % "1.7.3",
  )
