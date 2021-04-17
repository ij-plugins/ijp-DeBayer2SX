import xerial.sbt.Sonatype.GitHubHosting

import java.net.URL

// @formatter:off

name := "ijp-debayer2sx"

val _version = "1.2.0.1-SNAPSHOT"
val _scalaVersions = Seq("2.13.5", "2.12.13")
val _scalaVersion  = _scalaVersions.head

version         := _version
scalaVersion    := _scalaVersion
publishArtifact := false
publish / skip  := true
sonatypeProfileName := "net.sf.ij-plugins"

val commonSettings = Seq(
  version      := _version,
  organization := "net.sf.ij-plugins",
  homepage     := Some(new URL("https://github.com/ij-plugins/ijp-color")),
  startYear    := Some(2002),
  licenses     := Seq(("LGPL-2.1", new URL("http://opensource.org/licenses/LGPL-2.1"))),
  //
  crossScalaVersions := _scalaVersions,
  scalaVersion       := _scalaVersion,
  //
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-unchecked", 
    "-deprecation", 
    "-Xlint", 
    "-feature",
    "-explaintypes", 
  ),
  Compile / doc / scalacOptions ++= Opts.doc.title("IJP Debayer2SX API"),
  Compile / doc / scalacOptions ++= Opts.doc.version(_version),
  Compile / doc / scalacOptions ++= Seq(
    "-doc-footer", s"IJP Debayer2SX API v.${_version}",
    "-doc-root-content", baseDirectory.value + "/src/main/scala/root-doc.creole"
  ),
  Compile / doc / scalacOptions ++= (
    Option(System.getenv("GRAPHVIZ_DOT_PATH")) match {
      case Some(path) => Seq("-diagrams", "-diagrams-dot-path", path, "-diagrams-debug")
      case None => Seq.empty[String]
    }),
  javacOptions  ++= Seq("-deprecation", "-Xlint"),
  //
  libraryDependencies ++= Seq(
    "net.imagej"     % "ij"        % "1.53i",
    "org.scalatest" %% "scalatest" % "3.2.7" % "test",
  ),
  resolvers += Resolver.sonatypeRepo("snapshots"),
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
  sonatypeProfileName := "net.sf.ij-plugins",
  sonatypeProjectHosting := Some(GitHubHosting("ij-plugins", "ijp-debayer2sx", "jpsacha@gmail.com")),
  publishTo := sonatypePublishToBundle.value,
  developers := List(
    Developer(id="jpsacha", name="Jarek Sacha", email="jpsacha@gmail.com", url=url("https://github.com/jpsacha"))
  )
)

lazy val ijp_debayer2sx_core = project.in(file("ijp-debayer2sx-core"))
  .settings(
    name        := "ijp-debayer2sx-core",
    description := "IJP DeBayer2SX Core",
    commonSettings,
    libraryDependencies += "com.beachape" %% "enumeratum" % "1.6.1",
  )

lazy val ijp_debayer2sx_plugins = project.in(file("ijp-debayer2sx-plugins"))
  .settings(
    name        := "ijp-debayer2sx-plugins",
    description := "IJP DeBayer2SX ImageJ Plugins",
    commonSettings,
  )
  .dependsOn(ijp_debayer2sx_core)

lazy val ijp_debayer2sx_demos = project.in(file("ijp-debayer2sx-demos"))
  .settings(commonSettings,
    name        := "ijp-debayer2sx-demos",
    description := "IJP DeBayer2SX Demos",
    publishArtifact := false,
    publish / skip  := true)
  .dependsOn(ijp_debayer2sx_core)

lazy val ijp_debayer2sx_experimental = project.in(file("ijp-debayer2sx-experimental"))
  .settings(commonSettings,
    name        := "ijp-debayer2sx-experimental",
    description := "Experimental Features",
    publishArtifact := false,
    publish / skip  := true)
  .dependsOn(ijp_debayer2sx_core)

lazy val manifestSetting = packageOptions += {
  Package.ManifestAttributes(
    "Created-By" -> "Simple Build Tool",
    "Built-By"  -> Option(System.getenv("JAR_BUILT_BY")).getOrElse(System.getProperty("user.name")),
    "Build-Jdk" -> System.getProperty("java.version"),
    "Specification-Title"      -> name.value,
    "Specification-Version"    -> version.value,
    "Specification-Vendor"     -> organization.value,
    "Implementation-Title"     -> name.value,
    "Implementation-Version"   -> version.value,
    "Implementation-Vendor-Id" -> organization.value,
    "Implementation-Vendor"    -> organization.value
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