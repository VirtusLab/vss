package com.virtuslab.vss.cats.base.http

import org.http4s.HttpApp
import cats.effect.kernel.Resource
import com.comcast.ip4s._
import org.http4s.server.Server
import cats.effect.kernel.Async
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger
import com.virtuslab.vss.cats.base.config.BaseAppConfig

trait BaseHttpServer[F[_]]:
  def newServer(appConfig: BaseAppConfig, app: HttpApp[F]): Resource[F, Server]

object BaseHttpServer:
  def apply[F[_]: BaseHttpServer]: BaseHttpServer[F] = summon

  def printSwaggerMessage[F[_]: Logger](server: Server): F[Unit] =
    Logger[F].info(s"Go to http:/${server.address}/docs to open SwaggerUI for the Base service.")

  given forAsyncLogger[F[_]: Async: Logger]: BaseHttpServer[F] =
    new BaseHttpServer[F]:
      override def newServer(appConfig: BaseAppConfig, app: HttpApp[F]): Resource[F, Server] =
        EmberServerBuilder
          .default[F]
          .withHostOption(Host.fromString(appConfig.httpHost))
          .withPort(Port.fromInt(appConfig.httpPort).getOrElse(port"8080"))
          .withHttpApp(app)
          .build
          .evalTap(printSwaggerMessage[F])
