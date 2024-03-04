package com.virtuslab.vss.cats.base.config

import ciris.*
import cats.syntax.all.*
import cats.effect.*
import java.net.URI

object Config {
  /**
    * Loads the configuration from the environment variables, using ciris.
    */
  def load[F[_]: Async](): F[BaseAppConfig] =
    (
      env("BASE_HTTP_HOST").as[String].default("127.0.0.1"),
      env("BASE_HTTP_PORT").as[Int].default(8080),
      env("BASE_GRPC_HOST").as[String].default("127.0.0.1"),
      env("BASE_GRPC_PORT").as[Int].default(8081),
      env("BASE_DB_HOST").as[String].default("127.0.0.1"),
      env("BASE_DB_PORT").as[Int].default(5432),
      env("BASE_DB_NAME").as[String].default("vss"),
      env("BASE_DB_USER").as[String].default("postgres"),
      env("BASE_DB_PASSWORD").as[String].default("postgres"),
      env("KAFKA_HOST").as[String].default("127.0.0.1"),
      env("KAFKA_PORT").as[Int].default(9092),
      env("JEAGER_URI").as[String].default("http://localhost:16686")
    ).parMapN { (baseHttpHost, baseHttpPort, baseGrpcHost, baseGrpcPort, baseDbHost, baseDbPort, baseDbName, baseDbUser, baseDbPassword, kafkaHost, kafkaPort, jaegerUri) =>
      BaseAppConfig(
        baseHttpHost,
        baseHttpPort,
        baseGrpcHost,
        baseGrpcPort,
        baseDbHost,
        baseDbPort,
        baseDbName,
        baseDbUser,
        baseDbPassword,
        kafkaHost,
        kafkaPort,
        jaegerUri
      )
    }.load[F]

}
