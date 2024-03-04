package com.virtuslab.vss.cats.stats.http.routes

import cats.Monad
import cats.syntax.all.*
import cats.effect.kernel.Async
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import com.virtuslab.vss.common.*
import com.virtuslab.vss.cats.stats.services.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*

final case class StatsRoutes[F[_]: Monad: Async](
  stats: Stats[F]
) extends Http4sDsl[F]:
  val routes: List[ServerEndpoint[Any, F]] =
    List(
      StatsEndpoints.getLatestEvents.serverLogic[F] { rawPassword =>
        stats.getLatestEvents(100).attempt.map(_.leftMap(_ => ()))
      }
    )

  val docsRoutes =
    List(
      StatsEndpoints.getLatestEvents
    )
