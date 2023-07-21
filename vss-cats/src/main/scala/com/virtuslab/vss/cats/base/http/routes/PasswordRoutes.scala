package com.virtuslab.vss.cats.base.http.routes

import cats.Monad
import cats.syntax.all.*
import cats.effect.kernel.Async
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import com.virtuslab.vss.common.*
import com.virtuslab.vss.cats.base.services.*
import com.virtuslab.vss.common.BaseEndpoints
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import natchez.Trace

final case class PasswordRoutes[F[_]: Monad: Async: Trace](
  passwords: Passwords[F]
) extends Http4sDsl[F]:
  val routes: List[ServerEndpoint[Any, F]] =
    List(
      BaseEndpoints.hashPasswordEndpoint.serverLogic[F] { rawPassword =>
        passwords.hashPassword(rawPassword).attempt.map(_.leftMap(_ => ()))
      },
      BaseEndpoints.checkPasswordEndpoint.serverLogicSuccess[F] { checkData =>
        passwords.checkPwned(checkData)
      }
    )
  
  val docsRoutes: List[AnyEndpoint] =
    List(
      BaseEndpoints.hashPasswordEndpoint,
      BaseEndpoints.checkPasswordEndpoint
    )
