package com.virtuslab.vss.vanilla

import com.virtuslab.vss.common.HashAlgorithm
import com.virtuslab.vss.common.base.HashedPassword
import com.virtuslab.vss.common.base.{HashPasswordHttpEndpoints, HashedPassword}
import sttp.tapir.server.netty.NettyFutureServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object VanillaEndpoints:

  private val passwordServerEndpoint = HashPasswordHttpEndpoints.hashPasswordEndpoint.serverLogicSuccess(rawPassword =>
    Future.successful(
      HashedPassword(rawPassword.hashType, rawPassword.password, HashAlgorithm.hash(rawPassword.password))
    )
  )

  private val docs =
    SwaggerInterpreter().fromEndpoints[Future](List(passwordServerEndpoint.endpoint), "vss-vanilla", "1.0.0")

  val all = List(passwordServerEndpoint) ++ docs

class VanillaHttpServer():

  def runHttpServer(httpPort: Int): Future[Unit] =
    val program =
      for binding <- NettyFutureServer().port(httpPort).addEndpoints(VanillaEndpoints.all).start()
      yield ()

    program
