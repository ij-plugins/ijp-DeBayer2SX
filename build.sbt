import sbt.Keys.version
// @formatter:off

name := "ijp-debayer2sx"

val _version = "0.1-SNAPSHOT"

scalaVersion := "2.12.7"
publishArtifact := false

val commonSettings = Seq(
  organization := "net.sf.ij-plugins",
  version := _version,
  scalaVersion := "2.12.7",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint", "-explaintypes" /*, "-opt:l:method"*/),
  javacOptions ++= Seq("-deprecation", "-Xlint"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "net.imagej" % "ij" % "1.52h",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    "ImageJ Releases" at "http://maven.imagej.net/content/repositories/releases/",
    // Use local maven repo for local javacv builds
    Resolver.mavenLocal
  ),
  autoCompilerPlugins := true,
  // fork a new JVM for 'run' and 'test:run'
  fork := true,
  //
  manifestSetting,
  publishSetting
)

// Resolvers
lazy val sonatypeNexusSnapshots = Resolver.sonatypeRepo("snapshots")
lazy val sonatypeNexusStaging = "Sonatype Nexus Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

lazy val publishSetting = publishTo := {
  val version: String = _version
  if (version.trim.endsWith("SNAPSHOT"))
    Some(sonatypeNexusSnapshots)
  else
    Some(sonatypeNexusStaging)
}

lazy val ijp_debayer2sx_core = project.in(file("ijp-debayer2sx-core"))
  .settings(commonSettings,
    name := "ijp-debayer2sx-core",
    description := "IJP DeBayer2SX Core")

lazy val ijp_debayer2sx_plugins = project.in(file("ijp-debayer2sx-plugins"))
  .settings(commonSettings,
    name := "ijp-debayer2sx-plugins",
    description := "IJP DeBayer2SX ImageJ Plugins",
    publishArtifact := false)
  .dependsOn(ijp_debayer2sx_core)

lazy val ijp_debayer2sx_demos = project.in(file("ijp-debayer2sx-demos"))
  .settings(commonSettings,
    name := "ijp-debayer2sx-demos",
    description := "IJP DeBayer2SX Demos",
    publishArtifact := false)
  .dependsOn(ijp_debayer2sx_core)

lazy val ijp_debayer2sx_experimental = project.in(file("ijp-debayer2sx-experimental"))
  .settings(commonSettings,
    name := "ijp-debayer2sx-experimental",
    description := "Experimental Features",
    publishArtifact := false)
  .dependsOn(ijp_debayer2sx_core)

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> " }
publishArtifact := false

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
pomExtra :=
  <scm>
    <url>https://github.com/ij-plugins/ijp-toolkit</url>
    <connection>scm:https://github.com/ij-plugins/ijp-toolkit.git</connection>
  </scm>
    <developers>
      <developer>
        <id>jpsacha</id>
        <name>Jarek Sacha</name>
        <url>https://github.com/jpsacha</url>
      </developer>
    </developers>