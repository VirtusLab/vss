ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

val tapirVersion = "1.2.10"

lazy val root = (project in file("."))
  .settings(
    name := "vss-bootstrap"
  )
  .aggregate(vss_vanilla, vss_zio, vss_cats, commons)

val commonSettings = (
  scalacOptions ++= Seq(
    "-Ykind-projector"
  )
)

lazy val commons = (project in file("commons"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.4.5",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-upickle" % tapirVersion,
      "commons-codec" % "commons-codec" % "1.15",
      "com.softwaremill.sttp.client3" %% "upickle" % "3.8.11" % Test,
      "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test
    )
  )

lazy val vss_vanilla = (project in file("vss-vanilla"))
  .settings(commonSettings)
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion
    )
  )
  .dependsOn(commons)

lazy val vss_zio = (project in file("vss-zio"))
  .settings(commonSettings)
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value
    ),
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    )
  )
  .dependsOn(commons)

val http4sVersion = "0.23.18"
val fs2Version = "3.6.1"
val catsEffectVersion = "3.4.8"
val doobieVersion = "1.0.0-RC4"
val monocleVersion = "3.2.0"

lazy val vss_cats = project.in(file("vss-cats"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"   %% "cats-effect"         % catsEffectVersion,
      "co.fs2"          %% "fs2-core"            % fs2Version,
      "com.github.fd4s" %% "fs2-kafka"           % "3.0.1",
      "org.http4s"      %% "http4s-server"       % http4sVersion,
      "org.http4s"      %% "http4s-ember-server" % http4sVersion,
      "org.http4s"      %% "http4s-circe"        % http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % http4sVersion,
      "org.tpolecat"    %% "doobie-core"         % doobieVersion,
      "org.tpolecat"    %% "doobie-postgres"     % doobieVersion,
      "org.tpolecat"    %% "doobie-hikari"       % doobieVersion,
      "dev.optics"      %% "monocle-core"        % monocleVersion,
      "dev.optics"      %% "monocle-macro"       % monocleVersion,
      "is.cir"          %% "ciris"               % "3.1.0",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"   % tapirVersion,
      "org.tpolecat" %% "natchez-http4s" % "0.5.0",
      "org.tpolecat" %% "natchez-jaeger" % "0.3.0",
      "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
    )
  )
  .enablePlugins(Fs2Grpc)
  .dependsOn(commons)
