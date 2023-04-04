package com.virtuslab.vss.cats.stats.http

import cats.effect.kernel.Async
import com.virtuslab.vss.cats.stats.http.routes.*
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import com.virtuslab.vss.cats.stats.services.Services
import org.http4s.server.Router

object HttpApi:
  def make[F[_]: Async](
    services: Services[F]
  ): HttpApi[F] = new HttpApi[F](services) {}

sealed abstract class HttpApi[F[_]: Async](
  val services: Services[F]
):

  private val statsRoutes = StatsRoutes[F](services.stats).routes

  private val routes: HttpRoutes[F] = statsRoutes

  val httpApp: HttpApp[F] = routes.orNotFound
