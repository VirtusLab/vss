ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.0"

val tapirVersion = "1.2.10"

lazy val root = (project in file("."))
  .settings(
    name := "vss-bootstrap"
  )
  .aggregate(vss_vanilla, vss_zio, vss_cats, commons)

val commonSettings =
  scalacOptions ++= Seq(
    "-Ykind-projector"
  )

lazy val commons = (project in file("commons"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-upickle" % tapirVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.5",
      "commons-codec" % "commons-codec" % "1.15",
      "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
      "com.softwaremill.sttp.client3" %% "upickle" % "3.8.11" % Test,
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


val doobieVersion = "1.0.0-RC2"

lazy val vss_zio = (project in file("vss-zio"))
  .settings(commonSettings)
  .settings(
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value
    ),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.15",
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      ("io.getquill" %% "quill-zio" % "4.6.0.1").exclude("com.lihaoyi", "geny_2.13"),
      ("io.getquill" %% "quill-jdbc-zio" % "4.6.0.1").exclude("com.lihaoyi", "geny_2.13"),
        "org.postgresql"       %  "postgresql"     % "42.3.1",
      "dev.zio" %% "zio-logging" % "2.1.13",
      "dev.zio" %% "zio-opentracing" % "2.0.3",
      "io.opentracing" % "opentracing-api" % "0.33.0",
      "io.jaegertracing" % "jaeger-core" % "1.8.0",
      "io.jaegertracing" % "jaeger-client" % "1.8.0",
      "io.scalaland" %% "chimney" % "0.8.0-M1",
      "dev.zio" %% "zio-streams" % "2.0.9",
      "dev.zio" %% "zio-kafka" % "2.1.1"
    )
  )
  .dependsOn(commons)

val http4sVersion = "0.23.18"
val fs2Version = "3.6.1"
val catsEffectVersion = "3.4.8"
val monocleVersion = "3.2.0"

lazy val vss_cats = project
  .in(file("vss-cats"))
  .configure(setupCommonDockerImageConfig)
  .settings(commonSettings)
  .settings(
    dockerExposedPorts := Seq(8080, 8081, 8180, 8181),
    Docker / packageName := "vss-cats",
    Compile / mainClass := Some("com.virtuslab.vss.cats.Main"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "com.github.fd4s" %% "fs2-kafka" % "3.0.0-RC1",
      "org.http4s" %% "http4s-server" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "dev.optics" %% "monocle-core" % monocleVersion,
      "dev.optics" %% "monocle-macro" % monocleVersion,
      "is.cir" %% "ciris" % "3.1.0",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.tpolecat" %% "natchez-http4s" % "0.5.0",
      "org.tpolecat" %% "natchez-jaeger" % "0.3.0",
      "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
    )
  )
  .enablePlugins(DockerPlugin, JavaAppPackaging, Fs2Grpc)
  .dependsOn(commons)

lazy val infra = project
  .in(file("infra"))
  .settings(
    libraryDependencies ++= Seq(
      "org.virtuslab" %% "besom-kubernetes" % "0.0.1-SNAPSHOT",
      "org.virtuslab" %% "besom-core" % "0.0.1-SNAPSHOT"
    )
  )

def setupCommonDockerImageConfig(project: Project): Project =
  project
    .settings(
      dockerRepository := Some("localhost:5001"),
      dockerBaseImage := "eclipse-temurin:11.0.16.1_1-jdk-focal",
      Docker / aggregate := false,
      Compile / packageDoc / publishArtifact := false
    )
