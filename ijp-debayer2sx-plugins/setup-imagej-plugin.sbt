// Setup to sbt-imagej plugin

// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"

// Enable and customize `sbt-imagej` plugin
ijRuntimeSubDir := "sandbox"
ijPluginsSubDir := "ij-plugins"
ijCleanBeforePrepareRun := true
// Instruct `clean` to delete created plugins subdirectory created by `ijRun`/`ijPrepareRun`.
cleanFiles += ijPluginsDir.value
