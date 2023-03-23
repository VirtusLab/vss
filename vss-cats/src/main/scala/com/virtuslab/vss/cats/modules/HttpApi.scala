package com.virtuslab.vss.cats.modules

import cats.effect.kernel.Async
import com.virtuslab.vss.cats.http.routes.*
import org.http4s.HttpRoutes
import org.http4s.HttpApp

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
