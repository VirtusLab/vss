package com.virtuslab.vss.cats.stats.http

import cats.effect.kernel.Async
import com.virtuslab.vss.cats.stats.http.routes.*
import com.virtuslab.vss.cats.stats.services.Services
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import org.http4s.server.Router
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter

object HttpApi:
  /**
    * Create the combined routes for the whole application.
    */
  def make[F[_]: Async](
    services: Services[F]
  ): HttpRoutes[F] = {

  val passwordRoutes = StatsRoutes(services.stats)

  val routes: List[ServerEndpoint[Any, F]] = passwordRoutes.routes

  val docsRoutes: List[ServerEndpoint[Any, F]] =
    SwaggerInterpreter()
      .fromEndpoints(passwordRoutes.docsRoutes, "vss-stats-cats", "1.0.0")

  val combinedRoutes: List[ServerEndpoint[Any, F]] = routes ++ docsRoutes

  Http4sServerInterpreter[F]().toRoutes(combinedRoutes)
}
