ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

val tapirVersion = "1.2.10"

lazy val root = (project in file("."))
  .settings(
    name := "vss-bootstrap"
  )
  .aggregate(vss_vanilla, vss_zio, commons)

lazy val commons = (project in file("commons"))
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
