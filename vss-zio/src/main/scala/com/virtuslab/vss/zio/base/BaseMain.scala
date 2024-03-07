package com.virtuslab.vss.zio.base

import zio.*
import com.virtuslab.vss.zio.base.http.HttpService
import com.virtuslab.vss.zio.base.grpc.GrpcService
import com.virtuslab.vss.zio.base.services.PasswordService
import com.virtuslab.vss.zio.base.repositories.PasswordRepository
import com.virtuslab.vss.zio.base.resources.*

object BaseMain:
  private def app: RIO[HttpService & GrpcService, Unit] = for
    http <- ZIO.service[HttpService]
    grpc <- ZIO.service[GrpcService]
    _    <- ZIO.collectAllParDiscard(Vector(http.serve(), grpc.serve()))
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
