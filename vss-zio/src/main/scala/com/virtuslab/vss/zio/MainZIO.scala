package com.virtuslab.vss.zio

import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio._
import zio.http.{HttpApp, Server => HttpServer, ServerConfig => HttpServerConfig}
import scalapb.zio_grpc.{Server => GrpcServer}
import java.io.IOException
import com.virtuslab.vss.zio.ZioGrpcServer.GrpcServerConfig

object MainZIO extends ZIOAppDefault:

  def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =

    val httpPort = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
    val grpcPort = sys.env.get("GRPC_PORT").flatMap(_.toIntOption).getOrElse(8181)

    val program: ZIO[
      HttpServerConfig & HttpServer & GrpcServerConfig &
        GrpcServer, // requirements for our program (provided by layers)
      Throwable,
      Unit
    ] = for {
      // HTTP
      _ <- HttpServer.serve(
        ZioHttpServer.routes.withDefaultErrorResponse
      ) // run server, requires HttpServer as an environment
      httpConfig <- ZIO.service[HttpServerConfig]
      _ <- Console.printLine(s"Go to http://localhost:${httpConfig.port}/docs to open SwaggerUI")

      // GRPC
      _ <- ZIO.service[GrpcServer].flatMap(_.start) // fetch GrpcServer from the environment and run it
      grpcConfig <- ZIO.service[GrpcServerConfig] // fetch GrpcServerConfig only to print console log
      _ <- Console.printLine(s"GRPC server is up and running at http://localhost:${grpcConfig.port}/")
    } yield ()

    program
      .provide(
        // Layers are ZIO Dependency Injection framework.
        // Each produced by Layer `Result` (`ZLayer[_, _, Result]`) can later be referenced with `ZIO.service[Result]`
        // HTTP server layers
        HttpServerConfig.live(HttpServerConfig.default.port(httpPort)), // ZLayer[_, _, HttpServerConfig]
        HttpServer.live, // ZLayer[HttpServerConfig, _, HttpServer]
        // GRPC server layers
        ZLayer.succeed(GrpcServerConfig(grpcPort)),
        ZioGrpcServer.live // produces GrpcServer
      )
      .exitCode
