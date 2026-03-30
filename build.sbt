lazy val root = (project in file("."))
  .settings(
    name := "noergler",
    organization := "org.leo",
    version := "0.1",
    scalaVersion := "2.13.18",
    Compile/mainClass := Some("noergler.Noergler"),
    assembly/mainClass := Some("noergler.Noergler"),
    assembly/assemblyJarName := s"${name.value}-${version.value}.jar",


    libraryDependencies += "io.github.leoprover" %% "scala-tptp-parser" % "1.7.3",
  )
