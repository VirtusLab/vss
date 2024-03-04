package com.virtuslab.vss.zio.base.grpc

import zio.*
import scalapb.zio_grpc.*
import io.grpc.{ServerBuilder, Status, StatusException}
import io.grpc.protobuf.services.ProtoReflectionService
import com.virtuslab.vss.common.{CheckPwned, HashPassword}
import com.virtuslab.vss.proto.zio.password.*
import com.virtuslab.vss.zio.base.services.PasswordService
import com.virtuslab.vss.zio.base.resources.TracingOps.*
import com.virtuslab.vss.zio.base.resources.TracingOps
import com.virtuslab.vss.zio.base.config.GrpcConfig
import zio.telemetry.opentracing.OpenTracing
import GrpcOps.*

trait GrpcService:
  def serve(): Task[Unit]

case class GrpcServiceImpl(passwordService: PasswordService, tracing: OpenTracing) extends GrpcService:
  private case class HashPasswordService() extends ZioPassword.HashPasswordService:
    override def hashPassword(request: HashPasswordRequest): IO[StatusException, HashPasswordResponse] =
      passwordService
        .hashPassword(request.toDomain)
        .handleError
        .map(_.toGrpc)
        .root(tracing, "hash password grpc")

  private case class PwnedService() extends ZioPassword.PwnedService:
    override def checkPwned(request: CheckPwnedRequest): IO[StatusException, CheckPwnedResponse] =
      passwordService
        .checkPwned(request.toDomain)
        .handleError
        .map(_.toGrpc)
        .root(tracing, "check pwned grpc")

  def serve(): Task[Unit] = ZIO.scoped {
    for
      config <- ZIO.config(GrpcConfig.config)
      serverLayer = ServerLayer.fromServiceList(
        ServerBuilder
          .forPort(config.port)
          .addService(ProtoReflectionService.newInstance()),
        ServiceList.add(HashPasswordService()).add(PwnedService())
      )
      _ <- serverLayer.build *> ZIO.never
    yield ()
  }

object GrpcService:
  val layer: URLayer[PasswordService & OpenTracing, GrpcService] = ZLayer.fromFunction(GrpcServiceImpl.apply)
