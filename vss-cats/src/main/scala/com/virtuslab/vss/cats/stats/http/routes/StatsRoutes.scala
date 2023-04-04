package com.virtuslab.vss.cats.stats.http.routes

import org.http4s.dsl.Http4sDsl
import cats.Monad
import cats.syntax.all.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.HttpRoutes
import cats.effect.kernel.Async
import com.virtuslab.vss.common.*
import com.virtuslab.vss.cats.stats.services.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter

final case class StatsRoutes[F[_]: Monad: Async](
  stats: Stats[F]
) extends Http4sDsl[F]:
  private val httpRoutes: HttpRoutes[F] =
    Http4sServerInterpreter[F]()
      .toRoutes(List(
        StatsEndpoints.getAllEvents.serverLogic[F] { rawPassword =>
          stats.getAllEvents().attempt.map(_.leftMap(_ => ()))
        }
      ))
  
  private val docs =
    Http4sServerInterpreter[F]().toRoutes(
      SwaggerInterpreter()
        .fromEndpoints[F](List(
          StatsEndpoints.getAllEvents
        ),
        "vss-cats",
        "1.0.0"
      )
    )

  def routes: HttpRoutes[F] = httpRoutes <+> docs
