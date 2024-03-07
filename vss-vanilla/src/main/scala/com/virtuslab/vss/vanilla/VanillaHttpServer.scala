package com.virtuslab.vss.vanilla

import sttp.tapir.server.netty.NettyFutureServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.*
import com.virtuslab.vss.common.*
import sttp.tapir.server.ServerEndpoint

object VanillaEndpoints:

  private val passwordServerEndpoint = BaseEndpoints.hashPasswordEndpoint.serverLogicSuccess { rawPassword =>
    Future.successful(
      HashedPassword(rawPassword.hashType, rawPassword.password, HashAlgorithm.hash(rawPassword.password))
    )
  }

  private val docs =
    SwaggerInterpreter().fromEndpoints[Future](List(passwordServerEndpoint.endpoint), "vss-vanilla", "1.0.0")

  val all: List[ServerEndpoint[Any, Future]] = List(passwordServerEndpoint) ++ docs

class VanillaHttpServer()(using ExecutionContext):

  def runHttpServer(httpPort: Int): Future[Unit] =
    NettyFutureServer().port(httpPort).addEndpoints(VanillaEndpoints.all).start().map(_ => ())
