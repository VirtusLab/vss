package com.virtuslab.vss.cats.base.http

import cats.*
import cats.data.Kleisli
import cats.effect.*
import com.virtuslab.vss.cats.base.http.routes.*
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import com.virtuslab.vss.cats.base.services.Services
import natchez.{ Trace, Span }
import natchez.http4s.NatchezMiddleware
import natchez.EntryPoint
import natchez.http4s.implicits.*

object HttpApi:
  /**
    * Create the combined routes for the whole application.
    *
    * This is also the place to add any middleware that should be applied to all
    * routes.
    */
  def make[F[_]: Async: Trace](
    services: Services[F]
  ): HttpRoutes[F] = {
    val passwordRoutes = PasswordRoutes(services.passwords).routes

    val routes = passwordRoutes

    NatchezMiddleware.server(routes)
  }
