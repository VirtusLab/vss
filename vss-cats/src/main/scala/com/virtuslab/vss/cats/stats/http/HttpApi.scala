package com.virtuslab.vss.cats.stats.http

import cats.effect.kernel.Async
import com.virtuslab.vss.cats.stats.http.routes.*
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import com.virtuslab.vss.cats.stats.services.Services
import org.http4s.server.Router

object HttpApi:
  /**
    * Create the combined routes for the whole application.
    */
  def make[F[_]: Async](
    services: Services[F]
  ): HttpRoutes[F] = {

  val statsRoutes = StatsRoutes[F](services.stats).routes

  val routes: HttpRoutes[F] = statsRoutes

  routes
}
