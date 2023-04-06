package com.virtuslab.vss.zio

import sttp.tapir.PublicEndpoint
import sttp.tapir.ztapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.http.{HttpApp, Request, Response}
import zio.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.upickle.*
import sttp.tapir.ztapir
import sttp.tapir.Endpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.Future
import sttp.tapir.server.ServerEndpoint
import zio.http.Server
import java.io.IOException
import zio.http.ServerConfig
import com.virtuslab.vss.common.*

object ZioHttpServer {

  import sttp.tapir.server.ziohttp.ZioHttpInterpreter

  val zioHashPasswordEndpointServer: ztapir.ZServerEndpoint[Any, Any] =
    BaseEndpoints.hashPasswordEndpoint.zServerLogic[Any](rawPassword =>
      ZIO.succeed(HashedPassword(rawPassword.hashType, rawPassword.password, HashAlgorithm.hash(rawPassword.password)))
    )

  val docs: List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
    .fromServerEndpoints[Task](List(zioHashPasswordEndpointServer), "vss-zio", "1.0.0")

  val routes = ZioHttpInterpreter().toHttp(List(zioHashPasswordEndpointServer) ++ docs)

}
