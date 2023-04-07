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

object BaseMain {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run: IO[Unit] =
    AppResources.make[IO]()
      .flatMap { res =>
        val services = Services.make[IO](res.db, res.kafka)
        val httpApi = HttpApi.make[IO](services)
        (
          BaseHttpServer[IO].newServer(httpApi.httpApp),
          BaseGrpcServer[IO].newServer(services)
        ).parTupled.void
      }.useForever

}
