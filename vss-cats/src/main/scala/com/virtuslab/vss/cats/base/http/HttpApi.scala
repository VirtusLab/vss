package com.virtuslab.vss.cats.base.http

import cats.effect.kernel.Async
import com.virtuslab.vss.cats.base.http.routes.*
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import com.virtuslab.vss.cats.base.services.Services

object HttpApi:
  def make[F[_]: Async](
    services: Services[F]
  ): HttpApi[F] = new HttpApi[F](services) {}

sealed abstract class HttpApi[F[_]: Async](
  val services: Services[F]
):

  private val passwordRoutes = PasswordRoutes[F](services.passwords).routes

  private val routes: HttpRoutes[F] = passwordRoutes

  val httpApp: HttpApp[F] = routes.orNotFound
