logLevel := Level.Info

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.5")

addSbtPlugin("com.github.karasiq" % "sbt-gulp" % "1.0.1")

// `javacpp` are packaged with maven-plugin packaging, we need to make SBT aware that it should be added to class path.
classpathTypes += "maven-plugin"

// javacpp `Loader` is used to determine `platform` classifier in the project`s `build.sbt`
// We define dependency here (in folder `project`) since it is used by the build itself.
libraryDependencies += "org.bytedeco" % "javacpp" % "1.1"