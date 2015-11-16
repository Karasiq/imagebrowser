name := "imagebrowser"

isSnapshot := true

version := "1.0-SNAPSHOT"

organization := "com.github.karasiq"

scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("snapshots")

classpathTypes += "maven-plugin"

// Platform classifier for native library dependencies
val javacppVersion = "1.1"

val platform = org.bytedeco.javacpp.Loader.getPlatform

// Libraries with native dependencies
val bytedecoPresetLibs = Seq(
  "opencv" → s"3.0.0-$javacppVersion",
  "ffmpeg" → s"2.8.1-$javacppVersion"
).flatMap {
  case (lib, ver) ⇒ Seq(
    // Add both: dependency and its native binaries for the current `platform`
    "org.bytedeco.javacpp-presets" % lib % ver,
    "org.bytedeco.javacpp-presets" % lib % ver classifier platform
  )
}

libraryDependencies ++= bytedecoPresetLibs ++ {
  val akkaV = "2.3.11"
  val sprayV = "1.3.3"
  Seq(
    "com.github.karasiq" %% "akka-commons" % "1.0",
    "com.github.karasiq" %% "mapdbutils" % "1.1-SNAPSHOT",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-kernel" % akkaV,
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing" % sprayV,
    "io.spray" %% "spray-caching" % sprayV,
    "io.spray" %% "spray-testkit" % sprayV % "test",
    "io.spray" %% "spray-json" % "1.3.2",
    "org.mapdb" % "mapdb" % "2.0-beta8",
    "com.drewnoakes" % "metadata-extractor" % "2.8.1",
    "commons-io" % "commons-io" % "2.4",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "org.bytedeco" % "javacv" % javacppVersion
  )
}

autoCompilerPlugins := true

mainClass in Compile := Some("com.karasiq.imagebrowser.app.ImageBrowserBoot")

enablePlugins(JavaAppPackaging)

lazy val compileWebapp = taskKey[Unit]("Compiles web application")

compileWebapp in Compile := {
  import sys.process._
  Seq("webapp/make.bat").!
}

compile in Compile <<= (compile in Compile).dependsOn(compileWebapp in Compile)