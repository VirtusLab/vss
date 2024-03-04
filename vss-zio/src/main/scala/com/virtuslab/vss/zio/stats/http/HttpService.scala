package com.virtuslab.vss.zio.stats.http

import zio.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.http.{HttpApp, Server, ServerConfig}
import com.virtuslab.vss.zio.stats.config.HttpConfig
import com.virtuslab.vss.zio.stats.services.EventService
import com.virtuslab.vss.common.StatsEndpoints

trait HttpService:
  def serve(): Task[Unit]

case class HttpServiceImpl(eventService: EventService) extends HttpService:
  override def serve(): Task[Unit] = for
    config <- ZIO.config(HttpConfig.config)
    serverConfigLayer = ServerConfig.live(ServerConfig.default.port(config.port))
    serverLayer       = serverConfigLayer >>> Server.live
    _ <- ZIO.logInfo(
      s"Go to http:/${config.host}:${config.port}/docs to open SwaggerUI for the Base service."
    )
    _ <- Server.serve(routes.withDefaultErrorResponse).provide(serverLayer)
  yield ()

  private val getAllEvents: ZServerEndpoint[Any, Any] =
    StatsEndpoints.getLatestEvents.zServerLogic[Any](_ => eventService.listEvents().mapError(_ => ()))

  private val docs: List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
    .fromServerEndpoints[Task](List(getAllEvents), "vss-zio", "1.0.0")

  private val routes = ZioHttpInterpreter().toHttp(List(getAllEvents) ++ docs)

object HttpService:
  val layer: URLayer[EventService, HttpService] = ZLayer.fromFunction(HttpServiceImpl.apply)
