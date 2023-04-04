package com.virtuslab.vss.cats.base.http

import org.http4s.HttpApp
import cats.effect.kernel.Resource
import com.comcast.ip4s._
import org.http4s.server.Server
import cats.effect.kernel.Async
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger

trait BaseHttpServer[F[_]]:
  def newServer(app: HttpApp[F]): Resource[F, Server]

object BaseHttpServer:
  def apply[F[_]: BaseHttpServer]: BaseHttpServer[F] = summon

  val port: Port = port"8080"

  def printSwaggerMessage[F[_]: Logger](server: Server): F[Unit] =
    Logger[F].info(s"Go to http://localhost:${port.value}/docs to open SwaggerUI. Press ENTER key to exit.")

  given forAsyncLogger[F[_]: Async: Logger]: BaseHttpServer[F] =
    new BaseHttpServer[F]:
      override def newServer(app: HttpApp[F]): Resource[F, Server] =
        EmberServerBuilder
          .default[F]
          .withHost(host"0.0.0.0")
          .withPort(port)
          .withHttpApp(app)
          .build
          .evalTap(printSwaggerMessage[F])
