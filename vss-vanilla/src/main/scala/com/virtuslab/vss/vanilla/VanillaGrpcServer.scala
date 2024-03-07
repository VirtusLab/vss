package com.virtuslab.vss.vanilla

import com.virtuslab.vss.common.HashAlgorithm
import com.virtuslab.vss.proto.password.HashPasswordServiceGrpc
import com.virtuslab.vss.proto.password.{HashPasswordMessage, HashedPasswordMessage}
import io.grpc.ServerBuilder

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class HashPasswordGrpcServer()(using ec: ExecutionContext) extends HashPasswordServiceGrpc.HashPasswordService:
  def hashPassword(request: HashPasswordMessage): Future[HashedPasswordMessage] =
    Future(HashedPasswordMessage(request.hashType, request.password, HashAlgorithm.hash(request.password)))

class VanillaGrpcServer()(using ec: ExecutionContext):
  def runGrpcServer(grpcPort: Int): Future[Unit] =
    val server = ServerBuilder
      .forPort(grpcPort)
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
