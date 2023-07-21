package com.virtuslab.vss.cats.stats.http

import org.http4s.HttpApp
import cats.effect.kernel.Resource
import com.comcast.ip4s._
import org.http4s.server.Server
import cats.effect.kernel.Async
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import com.virtuslab.vss.cats.stats.config.StatsAppConfig

object StatsHttpServer:
  def printSwaggerMessage[F[_]: Logger](server: Server): F[Unit] =
    Logger[F].info(s"Go to http:/${server.address}/docs to open SwaggerUI for the Stats service.")

  def make[F[_]: Async: Logger](appConfig: StatsAppConfig, app: HttpApp[F]): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHostOption(Host.fromString(appConfig.httpHost))
      .withPort(Port.fromInt(appConfig.httpPort).getOrElse(port"8180"))
      .withHttpApp(app)
      .build
      .evalTap(printSwaggerMessage[F])
