package com.virtuslab.vss.zio

import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio._
import zio.http.{HttpApp, Server => HttpServer, ServerConfig}
import scalapb.zio_grpc.{Server => GrpcServer}
import java.io.IOException
import com.virtuslab.vss.zio.ZioGrpcServer.GrpcServerConfig

object MainZIO extends ZIOAppDefault:

  def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =

    val httpPort = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
    val grpcPort = sys.env.get("GRPC_PORT").flatMap(_.toIntOption).getOrElse(8181)

    val program: ZIO[ServerConfig & HttpServer & GrpcServerConfig & GrpcServer, Throwable, Unit] = for {
      _ <- HttpServer.serve(ZioHttpServer.routes.withDefaultErrorResponse)
      httpConfig <- ZIO.service[ServerConfig]
      _ <- Console.printLine(s"Go to http://localhost:${httpConfig.port}/docs to open SwaggerUI")
      _ <- ZIO.service[GrpcServer].flatMap(_.start)
      grpcConfig <- ZIO.service[GrpcServerConfig]
      _ <- Console.printLine(s"GRPC server is up and running at http://localhost:${grpcConfig.port}/ to open SwaggerUI")
    } yield ()

    program
      .provide(
        // HTTP server layers
        ServerConfig.live(ServerConfig.default.port(httpPort)),
        HttpServer.live, // produces HttpServer
        // GRPC server layers
        ZLayer.succeed(GrpcServerConfig(grpcPort)),
        ZioGrpcServer.live // produces GrpcServer
      )
      .exitCode
