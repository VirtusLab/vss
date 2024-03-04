package com.virtuslab.vss.cats.stats

import upickle.default.*
import cats.effect.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import com.virtuslab.vss.cats.stats.resources.AppResources
import com.virtuslab.vss.cats.stats.grpc.StatsGrpcServer
import com.virtuslab.vss.cats.stats.http.StatsHttpServer
import com.virtuslab.vss.cats.stats.http.HttpApi
import com.virtuslab.vss.cats.stats.services.Services
import com.virtuslab.vss.common.*
import com.virtuslab.vss.cats.stats.config.Config
import com.virtuslab.vss.cats.stats.kafka.EventsConsumer

/**
  * This object is responsible for starting the stats application. It loads the
  * configuration, creates resources and starts the servers.
  */
object StatsMain {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] = {
    for {
      appConfig <- Resource.eval(Config.load[IO]())
      res <- AppResources.make[IO](appConfig)
      services = Services.make[IO](res.eventsStore)
      httpApi = HttpApi.make[IO](services)
      _ <- StatsHttpServer.make[IO](appConfig, httpApi.orNotFound)
      _ <- StatsGrpcServer.make[IO](appConfig, services)
      _ <- EventsConsumer.runConsumer[IO](res.kafkaConsumer, services).background
    } yield ()
  }.useForever
}
