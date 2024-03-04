package com.virtuslab.vss.cats.stats.config

import cats.syntax.all.*
import cats.effect.*
import ciris.*

object Config {

  /**
    * Loads the configuration from the environment variables, using ciris.
    */
  def load[F[_] : Async](): F[StatsAppConfig] =
    (
      env("HTTP_HOST").as[String].default("127.0.0.1"),
      env("HTTP_PORT").as[Int].default(8180),
      env("GRPC_HOST").as[String].default("127.0.0.1"),
      env("GRPC_PORT").as[Int].default(8181),
      env("KAFKA_HOST").as[String].default("127.0.0.1"),
      env("KAFKA_PORT").as[Int].default(9092)
    ).parMapN { (httpHost, httpPort, grpcHost, grpcPort, kafkaHost, kafkaPort) =>
      StatsAppConfig(
        httpHost,
        httpPort,
        grpcHost,
        grpcPort,
        kafkaHost,
        kafkaPort
      )
    }.load[F]
}
