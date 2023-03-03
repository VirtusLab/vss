package com.virtuslab.vss.zio

import com.virtuslab.vss.common.HashPasswordHttpEndpoints
import sttp.tapir.PublicEndpoint
import sttp.tapir.ztapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.http.{HttpApp, Request, Response}
import zio.*
import com.virtuslab.vss.common.{HashPassword, HashedPassword}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.upickle.*
import com.virtuslab.vss.common.HashAlgorithm
import sttp.tapir.ztapir
import sttp.tapir.Endpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.Future
import sttp.tapir.server.ServerEndpoint

object ZioHttpServer {

  import sttp.tapir.server.ziohttp.ZioHttpInterpreter

  val zioHashPasswordEndpointServer: ztapir.ZServerEndpoint[Any, Any] =
    HashPasswordHttpEndpoints.hashPasswordEndpoint.zServerLogic[Any](rawPassword =>
      ZIO.succeed(HashedPassword(rawPassword.hashType, rawPassword.password, HashAlgorithm.hash(rawPassword.password)))
    )

  val docs: List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
    .fromServerEndpoints[Task](List(zioHashPasswordEndpointServer), "vss-zio", "1.0.0")

  def createZioHttpApp(): HttpApp[Any, Throwable] =
    ZioHttpInterpreter().toHttp(List(zioHashPasswordEndpointServer) ++ docs)

}
