package com.virtuslab.vss.cats.stats.http

import org.http4s.HttpApp
import cats.effect.kernel.Resource
import com.comcast.ip4s._
import org.http4s.server.Server
import cats.effect.kernel.Async
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.Logger

trait StatsHttpServer[F[_]]:
  def newServer(app: HttpApp[F]): Resource[F, Server]

object StatsHttpServer:
  def apply[F[_]: StatsHttpServer]: StatsHttpServer[F] = summon

  val port: Port = port"8180"

  def printSwaggerMessage[F[_]: Logger](server: Server): F[Unit] =
    Logger[F].info(s"Go to http://localhost:${port.value}/docs to open SwaggerUI for the Stats service.")

  given forAsyncLogger[F[_]: Async: Logger]: StatsHttpServer[F] =
    new StatsHttpServer[F]:
      override def newServer(app: HttpApp[F]): Resource[F, Server] =
        EmberServerBuilder
          .default[F]
          .withHost(host"0.0.0.0")
          .withPort(port)
          .withHttpApp(app)
          .build
          .evalTap(printSwaggerMessage[F])
