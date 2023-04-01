addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

val zioGrpcVersion = "0.6.0-rc4"
libraryDependencies ++= Seq(
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % zioGrpcVersion
)
