package com.virtuslab.vss.zio.base.http

import zio.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.http.{HttpApp, Server, ServerConfig}
import com.virtuslab.vss.zio.base.services.PasswordService
import com.virtuslab.vss.zio.base.resources.TracingOps.*
import com.virtuslab.vss.zio.base.resources.TracingOps
import com.virtuslab.vss.zio.base.config.HttpConfig
import zio.telemetry.opentracing.OpenTracing
import com.virtuslab.vss.common.{BaseEndpoints, HashAlgorithm, HashedPassword}

trait HttpService:
  def serve(): Task[Unit]

case class HttpServiceImpl(passwordService: PasswordService, tracing: OpenTracing) extends HttpService:
  override def serve(): Task[Unit] = for
    config <- ZIO.config(HttpConfig.config)
    serverConfigLayer = ServerConfig.live(ServerConfig.default.port(config.port))
    serverLayer       = serverConfigLayer >>> Server.live
    _ <- ZIO.logInfo(
      s"Go to http:/${config.host}:${config.port}/docs to open SwaggerUI for the Base service."
    )
    _ <- Server.serve(routes.withDefaultErrorResponse).provide(serverLayer)
  yield ()

  private val hashPasswordEndpoint: ZServerEndpoint[Any, Any] =
    BaseEndpoints.hashPasswordEndpoint.zServerLogic[Any] { rawPassword =>
      ZIO.logInfo(s"Got hash request: $rawPassword") *>
        passwordService.hashPassword(rawPassword).orDie.root(tracing, "hash password http")
    }

  private val checkPwnedEndpoint: ZServerEndpoint[Any, Any] =
    BaseEndpoints.checkPasswordEndpoint.zServerLogic[Any](checkData =>
      passwordService.checkPwned(checkData).orDie.root(tracing, "check pwned http")
    )

  private val docs: List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
    .fromServerEndpoints[Task](List(hashPasswordEndpoint, checkPwnedEndpoint), "vss-zio", "1.0.0")

  private val routes = ZioHttpInterpreter().toHttp(List(hashPasswordEndpoint, checkPwnedEndpoint) ++ docs)

object HttpService:
  val layer: URLayer[PasswordService & OpenTracing, HttpService] = ZLayer.fromFunction(HttpServiceImpl.apply)
