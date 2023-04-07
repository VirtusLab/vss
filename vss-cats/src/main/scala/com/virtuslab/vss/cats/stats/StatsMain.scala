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

object StatsMain {
  
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    Config.load[IO].flatMap { appConfig =>
      AppResources.make[IO](appConfig)
        .flatMap { res =>
          val services = Services.make[IO](res.eventsStore)
          val httpApi = HttpApi.make[IO](services)
          for {
            _ <- StatsHttpServer[IO].newServer(appConfig, httpApi.httpApp)
            _ <- StatsGrpcServer[IO].newServer(appConfig, services)
            _ <- Resource.eval(res.kafkaConsumer.subscribeTo("events"))
            _ <- res.kafkaConsumer.stream.evalTap { record =>
              services.stats.addEvent(record.record.value)
            }.compile.drain.background
          } yield ()
        }.useForever
    }
}
