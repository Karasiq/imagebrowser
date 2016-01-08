val commonSettings = Seq(
  organization := "com.github.karasiq",
  scalaVersion := "2.11.7",
  resolvers += Resolver.sonatypeRepo("snapshots"),
  isSnapshot := true,
  version := "1.0.0-M2-SNAPSHOT",
  scalacOptions ++= Seq("-target:jvm-1.8")
)

// Libraries with native dependencies
def javaCvLibs(javaCvVersion: String, libs: (String, String)*): Seq[ModuleID] = {
  val platforms = Seq("windows", "linux")
  val architectures = Seq("x86", "x86_64")

  (for {
    (lib, ver) <- libs
    os <- platforms
    arch <- architectures
  } yield Seq(
    // Add both: dependency and its native binaries for the current `platform`
    "org.bytedeco.javacpp-presets" % lib % s"$ver-$javaCvVersion",
    "org.bytedeco.javacpp-presets" % lib % s"$ver-$javaCvVersion" classifier s"$os-$arch"
  )).flatten
}

val backendDeps = {
  val javaCvVersion = "1.1"
  val akkaV = "2.4.0"
  val sprayV = "1.3.3"
  javaCvLibs(javaCvVersion, "opencv" → "3.0.0", "ffmpeg" → "2.8.1") ++ Seq(
    "com.github.karasiq" %% "commons-akka" % "1.0.3",
    "com.github.karasiq" %% "commons" % "1.0.3",
    "com.github.karasiq" %% "mapdbutils" % "1.1.1",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing-shapeless2" % sprayV,
    "io.spray" %% "spray-caching" % sprayV,
    "io.spray" %% "spray-testkit" % sprayV % "test",
    "io.spray" %% "spray-json" % "1.3.2",
    "org.mapdb" % "mapdb" % "2.0-beta12",
    "com.drewnoakes" % "metadata-extractor" % "2.8.1",
    "commons-io" % "commons-io" % "2.4",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "org.bytedeco" % "javacv" % javaCvVersion
  )
}

lazy val backendSettings = Seq(
  name := "imagebrowser",
  classpathTypes += "maven-plugin",
  libraryDependencies ++= backendDeps,
  autoCompilerPlugins := true,
  mainClass in Compile := Some("com.karasiq.imagebrowser.app.ImageBrowserBoot"),
  gulpDest in Compile := (resourceManaged in Compile).value / "webstatic"
)

lazy val backend = Project("imagebrowser", file("."))
  .enablePlugins(GulpPlugin, JavaAppPackaging)
  .settings(commonSettings, backendSettings)