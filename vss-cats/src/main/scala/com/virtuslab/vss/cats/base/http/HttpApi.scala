package com.virtuslab.vss.cats.base.http

import cats.*
import cats.implicits.*
import cats.data.Kleisli
import cats.effect.*
import com.virtuslab.vss.cats.base.http.routes.*
import com.virtuslab.vss.cats.base.services.Services
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import natchez.{Span, Trace}
import natchez.http4s.NatchezMiddleware
import natchez.EntryPoint
import natchez.http4s.implicits.*
import sttp.tapir.server.ServerEndpoint

object HttpApi:
  /**
    * Create the combined routes for the whole application.
    *
    * This is also the place to add any middleware that should be applied to all routes.
    */
  def make[F[_] : Async : Trace](
    services: Services[F]
  ): HttpRoutes[F] = {
    val passwordRoutes = PasswordRoutes(services.passwords)

    val routes: List[ServerEndpoint[Any, F]] = passwordRoutes.routes

    val docsRoutes: List[ServerEndpoint[Any, F]] =
      SwaggerInterpreter()
        .fromEndpoints(passwordRoutes.docsRoutes, "vss-cats", "1.0.0")

    val combinedRoutes: List[ServerEndpoint[Any, F]] = routes ++ docsRoutes

    NatchezMiddleware.server(
      Http4sServerInterpreter[F]().toRoutes(combinedRoutes)
    )
  }
