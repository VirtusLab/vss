package com.virtuslab.vss.cats.stats.http

import org.http4s.HttpApp
import cats.effect.kernel.Resource
import com.comcast.ip4s._
import org.http4s.server.Server
import cats.effect.kernel.Async
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import com.virtuslab.vss.cats.stats.config.StatsAppConfig
import cats.effect.IO

object StatsHttpServer:
  def printSwaggerMessage(server: Server)(using log: Logger[IO]): IO[Unit] =
    Logger[IO].info(s"Go to http:/${server.address}/docs to open SwaggerUI for the Stats service.")

  def make(appConfig: StatsAppConfig, app: HttpApp[IO])(using log: Logger[IO]): Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHostOption(Host.fromString(appConfig.httpHost))
      .withPort(Port.fromInt(appConfig.httpPort).getOrElse(port"8180"))
      .withHttpApp(app)
      .build
      .evalTap(printSwaggerMessage)
