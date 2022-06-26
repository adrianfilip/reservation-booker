ThisBuild / organization := "com.adrianfilip"
ThisBuild / name         := "booker"
ThisBuild / scalaVersion := "3.1.1"
ThisBuild / version      := "0.1.0"

val zioVersion        = "2.0.0"
val zioHttpVersion    = "2.0.0-RC5"
val zioJsonVersion    = "0.3.0-RC5"
//todo remove this if I end up not using it
val zioProcessVersion = "0.7.0-RC5"
val laminarVersion    = "0.14.2"

lazy val bookerService = project
  .in(file("booker-service"))
  .settings(
    name    := "booker-service",
    version := "0.1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "dev.zio"              %% "zio-json"   % zioJsonVersion,
      "io.d11"               %% "zhttp"      % zioHttpVersion,
      "com.github.jwt-scala" %% "jwt-core"   % "9.0.5",
      "io.d11"               %% "zhttp-test" % zioHttpVersion % Test
    )
  ).dependsOn(sharedDomain.jvm)

lazy val bookerUI = project
  .in(file("booker-ui"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name                            := "booker-ui",
    version                         := "0.1.0-SNAPSHOT",
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    scalaJSLinkerConfig ~= {
      _.withSourceMap(false)
    },
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time"           % "2.3.0",
      "io.github.cquiroz" %%% "scala-java-time-tzdb"      % "2.3.0",
      "org.scala-js"      %%% "scalajs-java-securerandom" % "1.0.0" cross CrossVersion.for3Use2_13,
      //zio
      "dev.zio"           %%% "zio"                       % zioVersion,
      "dev.zio"           %%% "zio-json"                  % zioJsonVersion,
      "dev.zio"           %%% "zio-prelude"               % "1.0.0-RC9",
      //laminar
      "com.raquo"         %%% "laminar"                   % laminarVersion,
      "io.laminext"       %%% "fetch"                     % "0.14.3"
    )
  )
  .dependsOn(sharedDomain.js)

lazy val sharedDomain =
  crossProject(JSPlatform, JVMPlatform)
    .in(file("shared-domain"))
    .settings(libraryDependencies ++= Seq("dev.zio" %%% "zio-json" % zioJsonVersion))
//    .jvmSettings(
//      // Add JVM-specific settings here
//      libraryDependencies ++= Seq("dev.zio" %% "zio-json" % zioJsonVersion)
//    )
//    .jsSettings(
//      // Add JS-specific settings here
////      scalaJSUseMainModuleInitializer := true,
//      libraryDependencies ++= Seq("dev.zio" %%% "zio-json" % zioJsonVersion)
//    )
