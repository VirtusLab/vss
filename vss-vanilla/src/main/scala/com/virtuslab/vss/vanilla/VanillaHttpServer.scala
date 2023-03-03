package com.virtuslab.vss.vanilla

import com.virtuslab.vss.common.{HashAlgorithm, HashPasswordHttpEndpoints, HashedPassword}
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

  def runHttpServer(): Future[Unit] =
    val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
    val program =
      for
        binding <- NettyFutureServer().port(port).addEndpoints(VanillaEndpoints.all).start()
        _ <- Future {
          println(s"Go to http://localhost:${binding.port}/docs to open SwaggerUI. Press ENTER key to exit.")
        }
        stop <- binding.stop()
      yield stop

    program
