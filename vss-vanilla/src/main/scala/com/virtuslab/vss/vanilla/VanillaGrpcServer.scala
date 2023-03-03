package com.virtuslab.vss.vanilla

import com.virtuslab.vss.common.HashAlgorithm
import com.virtuslab.vss.proto.password.HashPasswordServiceGrpc
import com.virtuslab.vss.proto.password.{HashPasswordMessage, HashedPasswordMessage}
import io.grpc.ServerBuilder

import scala.concurrent.{ExecutionContext, Future}

class HashPasswordGrpcServer()(implicit ec: ExecutionContext) extends HashPasswordServiceGrpc.HashPasswordService:

  def hashPassword(request: HashPasswordMessage): Future[HashedPasswordMessage] =
    Future(HashedPasswordMessage(request.hashType, request.password, HashAlgorithm.hash(request.password)))

class VanillaGrpcServer():
  def runGrpcServer(): Future[Unit] =
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
    val server = ServerBuilder
      .forPort(8181)
      .addService(HashPasswordServiceGrpc.bindService(new HashPasswordGrpcServer(), ec))
      .build
      .start
    println(s"gRPC server available at localhost:8181")
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      server.shutdown()
      System.err.println("*** gRPC server shut down")
    }
    Future(server.awaitTermination())
