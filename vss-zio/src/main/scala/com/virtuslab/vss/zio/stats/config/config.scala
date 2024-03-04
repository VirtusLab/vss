package com.virtuslab.vss.zio.stats.config

import zio.*

case class HttpConfig(host: String, port: Int)
object HttpConfig:
  val config: Config[HttpConfig] = (
    Config.string("HTTP_HOST").withDefault("127.0.0.1")
      ++ Config.int("HTTP_PORT").withDefault(8180)
  ).map(HttpConfig.apply)

case class GrpcConfig(host: String, port: Int)
object GrpcConfig:
  val config: Config[GrpcConfig] = (
    Config.string("GRPC_HOST").withDefault("127.0.0.1")
      ++ Config.int("GRPC_PORT").withDefault(8181)
  ).map(GrpcConfig.apply)

case class KafkaConfig(host: String, port: Int)
object KafkaConfig:
  val config: Config[KafkaConfig] = (
    Config.string("KAFKA_HOST").withDefault("127.0.0.1")
      ++ Config.int("KAFKA_PORT").withDefault(9092)
  ).map(KafkaConfig.apply)
