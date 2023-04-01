package com.virtuslab.vss.vanilla

import com.virtuslab.vss.common.{HashAlgorithm, HashPasswordHttpEndpoints, HashedPassword}
import com.virtuslab.vss.proto.password.HashPasswordServiceGrpc
import com.virtuslab.vss.proto.password.{HashPasswordMessage, HashedPasswordMessage}
import io.grpc.ServerBuilder
import sttp.tapir.server.netty.NettyFutureServer
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn
import scala.util.Failure
import scala.util.Success

@main def mainVanilla(): Unit =
  val httpPort = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
  val httpServer = new VanillaHttpServer().runHttpServer(httpPort)
  println(s"Go to http://localhost:$httpPort/docs to open SwaggerUI")

  val grpcPort = sys.env.get("GRPC_PORT").flatMap(_.toIntOption).getOrElse(8181)
  val grpcServer = new VanillaGrpcServer().runGrpcServer(grpcPort)
  println(s"GRPC server is up and running at http://localhost:$grpcPort/ to open SwaggerUI")

  Await.result(Future.sequence(List(httpServer, grpcServer)), Duration.Inf)
