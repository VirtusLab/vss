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

@main def mainVanilla(): Unit =
  val httpServer = new VanillaHttpServer().runHttpServer()
  val grpcServer = new VanillaGrpcServer().runGrpcServer()
  Await.result(Future.sequence(List(httpServer, grpcServer)), Duration.Inf)
