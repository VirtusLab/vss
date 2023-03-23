package com.virtuslab.vss.cats.http.routes

import org.http4s.dsl.Http4sDsl
import cats.Monad
import cats.syntax.all.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes
import cats.effect.kernel.Async
import com.virtuslab.vss.common.*
import com.virtuslab.vss.cats.services.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import com.virtuslab.vss.common.CheckPasswordHttpEndpoints
final case class PasswordRoutes[F[_]: Monad: Async](
  passwords: Passwords[F]
) extends Http4sDsl[F]:
  private val httpRoutes: HttpRoutes[F] =
    Http4sServerInterpreter[F]()
      .toRoutes(List(
        HashPasswordHttpEndpoints.hashPasswordEndpoint.serverLogic[F] { rawPassword =>
          passwords.hashPassword(rawPassword).attempt.map(_.leftMap(_ => ()))
        },
        CheckPasswordHttpEndpoints.checkPasswordEndpoint.serverLogicSuccess[F] { checkData =>
          passwords.checkPassword(checkData)
        }
      ))
  
  private val docs =
    Http4sServerInterpreter[F]().toRoutes(
      SwaggerInterpreter()
        .fromEndpoints[F](List(
          HashPasswordHttpEndpoints.hashPasswordEndpoint,
          CheckPasswordHttpEndpoints.checkPasswordEndpoint
        ),
        "vss-cats",
        "1.0.0"
      )
    )

  def routes: HttpRoutes[F] = httpRoutes <+> docs
