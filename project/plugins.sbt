addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
libraryDependencies ++= Seq("com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.6.0-rc4")
addSbtPlugin("org.typelevel" % "sbt-fs2-grpc" % "2.5.11")
addSbtPlugin("com.github.sbt" %% "sbt-native-packager" % "1.9.16")
