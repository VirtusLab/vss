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
import cats.effect.IO

final case class StatsRoutes(
  stats: Stats
) extends Http4sDsl[IO]:
  val routes: List[ServerEndpoint[Any, IO]] =
    List(
      StatsEndpoints.getLatestEvents.serverLogic[IO] { rawPassword =>
        stats.getLatestEvents(100).attempt.map(_.leftMap(_ => ()))
      }
    )

  val docsRoutes =
    List(
      StatsEndpoints.getLatestEvents
    )
