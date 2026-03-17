name := "ijp-debayer2sx"

//
// Environment variables used by the build:
// GRAPHVIZ_DOT_PATH - Full path to Graphviz dot utility. If not defined, Scaladoc will be built without diagrams.
// JAR_BUILT_BY      - Name to be added to Jar metadata field "Built-By" (defaults to System.getProperty("user.name")
//

ThisBuild / version := "1.3.5"
ThisBuild / scalaVersion := "3.3.7"
ThisBuild / organization := "net.sf.ij-plugins"
ThisBuild / homepage := Some(url("https://github.com/ij-plugins/ijp-color"))
ThisBuild / startYear := Some(2002)
ThisBuild / licenses := Seq("LGPL-2.1" -> url("https://opensource.org/licenses/LGPL-2.1"))
ThisBuild / organizationHomepage := Some(url("https://github.com/ij-plugins"))
ThisBuild / scmInfo := Option(
  ScmInfo(
    url("https://github.com/ij-plugins/ijp-DeBayer2SX/issues"),
    "scm:https://github.com/ij-plugins/ijp-DeBayer2SX.git"
  )
)

// Resolvers
// Add snapshots to the root project to enable compilation with Scala SNAPSHOT compiler,
// e.g., 2.11.0-SNAPSHOT
ThisBuild / resolvers += Resolver.sonatypeCentralSnapshots
ThisBuild / resolvers += Resolver.mavenLocal

publishArtifact := false
publish / skip := true

// Set the Java version target for compatibility for the current FIJI distribution
// We do not want to be over the FIJI Java version.
lazy val javaTargetVersion = "21"

lazy val libCFor = "io.github.metarank" %% "cfor" % "0.3"
lazy val libImageJ = "net.imagej" % "ij" % "1.54p"
lazy val libScalaTest = "org.scalatest" %% "scalatest" % "3.2.19"

lazy val ijp_debayer2sx_core = project.in(file("ijp-debayer2sx-core"))
  .settings(
    name := "ijp-debayer2sx-core",
    description := "IJP DeBayer2SX Core",
    commonSettings,
    libraryDependencies += libCFor
  )

lazy val ijp_debayer2sx_plugins = project.in(file("ijp-debayer2sx-plugins"))
  .settings(
    name := "ijp-debayer2sx-plugins",
    description := "IJP DeBayer2SX ImageJ Plugins",
    commonSettings
  )
  .dependsOn(ijp_debayer2sx_core)

lazy val ijp_debayer2sx_demos = project.in(file("ijp-debayer2sx-demos"))
  .settings(
    name := "ijp-debayer2sx-demos",
    description := "IJP DeBayer2SX Demos",
    commonSettings,
    publishArtifact := false,
    publish / skip := true
  )
  .dependsOn(ijp_debayer2sx_core)

lazy val ijp_debayer2sx_experimental = project.in(file("ijp-debayer2sx-experimental"))
  .settings(
    name := "ijp-debayer2sx-experimental",
    description := "Experimental Features",
    commonSettings,
    publishArtifact := false,
    publish / skip := true
  )
  .dependsOn(ijp_debayer2sx_core)

// Common settings
lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-explain",
    "-explain-types",
    "-rewrite",
    "-source:3.3-migration",
    "-Wunused:all",
    "-release",
    javaTargetVersion
  ),
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
  Compile / compile / javacOptions ++= Seq("-deprecation", "-Xlint", "--release", javaTargetVersion),
  //
  libraryDependencies ++= Seq(
    libImageJ,
    libScalaTest % "test"
  ),
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
  manifestSetting
)

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

//
// Customize Java style publishing
//
// Enables publishing to maven repo
ThisBuild / publishMavenStyle := true
ThisBuild / Test / publishArtifact := false
ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
ThisBuild / developers := List(
  Developer(
    id = "jpsacha",
    name = "Jarek Sacha",
    email = "jpsacha@gmail.com",
    url = url("https://github.com/jpsacha")
  )
)

// Enable and customize `sbt-imagej` plugin
enablePlugins(SbtImageJ)
ijRuntimeSubDir         := "sandbox"
ijPluginsSubDir         := "ij-plugins"
ijCleanBeforePrepareRun := true
// Instruct `clean` to delete created plugins subdirectory created by `ijRun`/`ijPrepareRun`.
cleanFiles += ijPluginsDir.value

addCommandAlias("ijRun", "ijp_debayer2sx_plugins/ijRun")
