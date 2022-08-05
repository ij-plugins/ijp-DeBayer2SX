import xerial.sbt.Sonatype.GitHubHosting

import java.net.URL

name := "ijp-debayer2sx"

ThisBuild / version := "1.3.3.1-SNAPSHOT"
ThisBuild / organization := "net.sf.ij-plugins"
ThisBuild / sonatypeProfileName := "net.sf.ij-plugins"
ThisBuild / homepage := Some(new URL("https://github.com/ij-plugins/ijp-color"))
ThisBuild / startYear := Some(2002)
ThisBuild / licenses := Seq(("LGPL-2.1", new URL("https://opensource.org/licenses/LGPL-2.1")))

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / crossScalaVersions := Seq("2.13.8", "2.12.16", "3.0.2")

publishArtifact := false
publish / skip := true

/** Return `true` if scala version corresponds to Scala 2, `false` otherwise */
def isScala2(scalaVersion: String): Boolean = {
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) => true
    case _ => false
  }
}

val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-release",
    "8"
  ) ++ (
    if (isScala2(scalaVersion.value))
      Seq(
        "-Xlint",
        "-explaintypes",
        "-Xsource:3"
      )
    else
      Seq(
        "-explain"
      )
    ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-target:8")
      case _ => Seq.empty[String]
    }
  },
  Compile / doc / scalacOptions ++= Opts.doc.title("IJP Debayer2SX API"),
  Compile / doc / scalacOptions ++= Opts.doc.version(version.value),
  Compile / doc / scalacOptions ++= Seq(
    "-doc-footer",
    s"IJP Debayer2SX API v.${version.value}",
    "-doc-root-content",
    baseDirectory.value + "/src/main/scala/root-doc.creole"
  ),
  Compile / doc / scalacOptions ++= (
    Option(System.getenv("GRAPHVIZ_DOT_PATH")) match {
      case Some(path) => Seq("-diagrams", "-diagrams-dot-path", path, "-diagrams-debug")
      case None => Seq.empty[String]
    }
    ),
  Compile / compile / javacOptions ++= Seq("-deprecation", "-Xlint", "--release", "8"),
  //
  libraryDependencies ++= Seq(
    "net.imagej" % "ij" % "1.53s",
    "org.scalatest" %% "scalatest" % "3.2.11" % "test"
  ),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  //
  exportJars := true,
  //
  autoCompilerPlugins := true,
  // Fork a new JVM for 'run' and 'test:run'
  fork := true,
  // Add a JVM option to use when forking a JVM for 'run'
  javaOptions += "-Xmx1G",
  // Instruct `clean` to delete created plugins subdirectory created by `ijRun`/`ijPrepareRun`.
  cleanFiles += ijPluginsDir.value,
  //
  manifestSetting,
  // Setup publishing
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(GitHubHosting("ij-plugins", "ijp-debayer2sx", "jpsacha@gmail.com")),
  publishTo := sonatypePublishToBundle.value,
  developers := List(
    Developer(
      id = "jpsacha",
      name = "Jarek Sacha",
      email = "jpsacha@gmail.com",
      url = url("https://github.com/jpsacha")
    )
  )
)

lazy val ijp_debayer2sx_core = project.in(file("ijp-debayer2sx-core"))
  .settings(
    name := "ijp-debayer2sx-core",
    description := "IJP DeBayer2SX Core",
    commonSettings,
    libraryDependencies ++= {
      if (isScala2(scalaVersion.value)) {
        Seq(
          "com.beachape" %% "enumeratum" % "1.7.0",
          "io.github.metarank" %% "cfor" % "0.2"
        )
      } else {
        Seq.empty[ModuleID]
      }
    }
  )

lazy val ijp_debayer2sx_plugins = project.in(file("ijp-debayer2sx-plugins"))
  .settings(
    name        := "ijp-debayer2sx-plugins",
    description := "IJP DeBayer2SX ImageJ Plugins",
    commonSettings
  )
  .dependsOn(ijp_debayer2sx_core)

lazy val ijp_debayer2sx_demos = project.in(file("ijp-debayer2sx-demos"))
  .settings(
    commonSettings,
    name := "ijp-debayer2sx-demos",
    description := "IJP DeBayer2SX Demos",
    publishArtifact := false,
    publish / skip := true
  )
  .dependsOn(ijp_debayer2sx_core)

lazy val ijp_debayer2sx_experimental = project.in(file("ijp-debayer2sx-experimental"))
  .settings(
    commonSettings,
    name := "ijp-debayer2sx-experimental",
    description := "Experimental Features",
    publishArtifact := false,
    publish / skip := true
  )
  .dependsOn(ijp_debayer2sx_core)

lazy val manifestSetting = packageOptions += {
  Package.ManifestAttributes(
    "Created-By" -> "Simple Build Tool",
    "Built-By" -> Option(System.getenv("JAR_BUILT_BY")).getOrElse(System.getProperty("user.name")),
    "Build-Jdk" -> System.getProperty("java.version"),
    "Specification-Title" -> name.value,
    "Specification-Version" -> version.value,
    "Specification-Vendor" -> organization.value,
    "Implementation-Title" -> name.value,
    "Implementation-Version" -> version.value,
    "Implementation-Vendor-Id" -> organization.value,
    "Implementation-Vendor" -> organization.value
  )
}

// Enable and customize `sbt-imagej` plugin
enablePlugins(SbtImageJ)
ijRuntimeSubDir         := "sandbox"
ijPluginsSubDir         := "ij-plugins"
ijCleanBeforePrepareRun := true
// Instruct `clean` to delete created plugins subdirectory created by `ijRun`/`ijPrepareRun`.
cleanFiles += ijPluginsDir.value

addCommandAlias("ijRun", "ijp_debayer2sx_plugins/ijRun")
