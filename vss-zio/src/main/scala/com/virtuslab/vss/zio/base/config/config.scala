package com.virtuslab.vss.zio.base.config

import zio.*

case class HttpConfig(host: String, port: Int)
object HttpConfig:
  val config: Config[HttpConfig] = (
    Config.string("BASE_HTTP_HOST").withDefault("127.0.0.1")
      ++ Config.int("BASE_HTTP_PORT").withDefault(8080)
  ).map(HttpConfig.apply)

case class GrpcConfig(host: String, port: Int)
object GrpcConfig:
  val config: Config[GrpcConfig] = (
    Config.string("BASE_GRPC_HOST").withDefault("127.0.0.1")
      ++ Config.int("BASE_GRPC_PORT").withDefault(8081)
  ).map(GrpcConfig.apply)

case class DbConfig(
  postgresHost: String,
  postgresPort: Int,
  dbName: String,
  postgresUser: String,
  postgresPassword: String
)
object DbConfig:
  val config: Config[DbConfig] = (
    Config.string("BASE_DB_HOST").withDefault("127.0.0.1")
      ++ Config.int("BASE_DB_PORT").withDefault(5432)
      ++ Config.string("BASE_DB_NAME").withDefault("vss")
      ++ Config.string("BASE_DB_USER").withDefault("postgres")
      ++ Config.string("BASE_DB_PASSWORD").withDefault("postgres")
  ).map(DbConfig.apply)

case class KafkaConfig(host: String, port: Int)
object KafkaConfig:
  val config: Config[KafkaConfig] = (
    Config.string("KAFKA_HOST").withDefault("127.0.0.1")
      ++ Config.int("KAFKA_PORT").withDefault(9092)
  ).map(KafkaConfig.apply)

case class JaegerConfig(uri: String)
object JaegerConfig:
  val config: Config[JaegerConfig] =
    Config.string("JEAGER_URI").withDefault("http://localhost:6831").map(JaegerConfig.apply)
