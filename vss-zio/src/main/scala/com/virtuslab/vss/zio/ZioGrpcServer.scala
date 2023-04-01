package com.virtuslab.vss.zio

import com.virtuslab.vss.proto.password.HashPasswordServiceGrpc.HashPasswordService
import scala.concurrent.Future
import com.virtuslab.vss.proto.password.HashPasswordMessage
import com.virtuslab.vss.proto.password.HashedPasswordMessage
import com.virtuslab.vss.proto.password.ZioPassword
import zio.IO
import io.grpc.StatusException
import zio.ZIO
import com.virtuslab.vss.common.HashAlgorithm
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import zio.ZLayer
import scalapb.zio_grpc.ServerLayer
import scalapb.zio_grpc.Server
import scalapb.zio_grpc.ScopedServer

object HashPasswordServiceImpl extends ZioPassword.HashPasswordService {

  override def hashPassword(request: HashPasswordMessage): IO[StatusException, HashedPasswordMessage] =
    ZIO.succeed(HashedPasswordMessage(request.hashType, request.password, HashAlgorithm.hash(request.password)))

}

object ZioGrpcServer {

  case class GrpcServerConfig(port: Int)

  def services: ServiceList[Any] = ServiceList.add(HashPasswordServiceImpl)

  def builder(grpcServerConfig: GrpcServerConfig) =
    ServerBuilder.forPort(grpcServerConfig.port).addService(ProtoReflectionService.newInstance())

  def live: ZLayer[GrpcServerConfig, Throwable, Server] = ZLayer.scoped {
    for {
      grpcServerConfig <- ZIO.service[GrpcServerConfig]
      server <- ScopedServer.fromServiceList(builder(grpcServerConfig), services)
    } yield server
  }

}
