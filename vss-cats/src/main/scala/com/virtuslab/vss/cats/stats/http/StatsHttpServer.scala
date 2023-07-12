package com.virtuslab.vss.cats.stats.http

import org.http4s.HttpApp
import cats.effect.kernel.Resource
import com.comcast.ip4s._
import org.http4s.server.Server
import cats.effect.kernel.Async
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import com.virtuslab.vss.cats.stats.config.StatsAppConfig

trait StatsHttpServer[F[_]]:
  def newServer(appConfig: StatsAppConfig, app: HttpApp[F]): Resource[F, Server]

object StatsHttpServer:
  def apply[F[_]: StatsHttpServer]: StatsHttpServer[F] = summon

  def printSwaggerMessage[F[_]: Logger](server: Server): F[Unit] =
    Logger[F].info(s"Go to http:/${server.address}/docs to open SwaggerUI for the Stats service.")

  /**
    * Default HTTP server instance for any effect type that has instances of `Async`, `Logger` and `Trace`.
    */
  given forAsyncLogger[F[_]: Async: Logger]: StatsHttpServer[F] =
    new StatsHttpServer[F]:
      override def newServer(appConfig: StatsAppConfig, app: HttpApp[F]): Resource[F, Server] =
        EmberServerBuilder
          .default[F]
          .withHostOption(Host.fromString(appConfig.httpHost))
          .withPort(Port.fromInt(appConfig.httpPort).getOrElse(port"8180"))
          .withHttpApp(app)
          .build
          .evalTap(printSwaggerMessage[F])
