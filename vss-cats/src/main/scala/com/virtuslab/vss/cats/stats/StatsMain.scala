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

object StatsMain {
  
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    Config.load[IO].flatMap { appConfig =>
      AppResources.make[IO](appConfig)
        .flatMap { res =>
          val services = Services.make[IO](res.eventsStore)
          val httpApi = HttpApi.make[IO](services)
          for {
            _ <- StatsHttpServer[IO].newServer(appConfig, httpApi.orNotFound)
            _ <- StatsGrpcServer[IO].newServer(appConfig, services)
            _ <- EventsConsumer[IO].runConsumer(res.kafkaConsumer, services).background
          } yield ()
        }.useForever
    }
}
