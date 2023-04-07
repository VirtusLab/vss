package com.virtuslab.vss.cats.base

import cats.effect.*
import cats.implicits.*
import com.virtuslab.vss.cats.base.resources.*
import cats.effect.std.Supervisor
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import com.virtuslab.vss.cats.base.http.BaseHttpServer
import com.virtuslab.vss.cats.base.grpc.BaseGrpcServer
import com.virtuslab.vss.cats.base.services.Services
import com.virtuslab.vss.cats.base.http.HttpApi
import com.virtuslab.vss.cats.base.config.Config

object BaseMain {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    Config.load[IO]().flatMap { appConfig =>
      AppResources.make[IO](appConfig)
        .flatMap { res =>
          val services = Services.make[IO](res.db, res.kafka)
          val httpApi = HttpApi.make[IO](services)
          (
            BaseHttpServer[IO].newServer(appConfig, httpApi.httpApp),
            BaseGrpcServer[IO].newServer(appConfig, services)
          ).parTupled.void
        }.useForever
    }
}
