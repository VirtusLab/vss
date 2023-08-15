package com.virtuslab.vss.zio.base

import zio.*
import com.virtuslab.vss.zio.base.http.HttpService
import com.virtuslab.vss.zio.base.grpc.GrpcService
import com.virtuslab.vss.zio.base.services.PasswordService
import com.virtuslab.vss.zio.base.repositories.PasswordRepository
import com.virtuslab.vss.zio.base.resources.*

object BaseMain:
  private def app: RIO[Any & HttpService & GrpcService, Unit] = for
    http <- ZIO.service[HttpService]
    httpServer <- http.serve().fork
    grpc <- ZIO.service[GrpcService]
    grpcServer <- grpc.serve().fork
    _ <- httpServer.zip(grpcServer).join
  yield ()

  def run: Task[Unit] = app.provide(
      HttpService.layer,
      GrpcService.layer,
      PasswordService.layer,
      PasswordRepository.layer,
      Db.layer,
      KafkaProducer.layer,
      Tracer.layer
    )
