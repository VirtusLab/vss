package com.virtuslab.vss.zio.stats

import zio.*
import com.virtuslab.vss.zio.stats.http.HttpService
import com.virtuslab.vss.zio.stats.grpc.GrpcService
import com.virtuslab.vss.zio.stats.services.EventService
import com.virtuslab.vss.zio.stats.resources.KafkaConsumer

object StatsMain:
  private def app: RIO[HttpService & GrpcService & KafkaConsumer, Unit] = for
    http  <- ZIO.service[HttpService]
    grpc  <- ZIO.service[GrpcService]
    kafka <- ZIO.service[KafkaConsumer]
    _     <- ZIO.collectAllParDiscard(Vector(http.serve(), grpc.serve(), kafka.consume.runDrain))
  yield ()

  def run: Task[Unit] = app.provide(
    HttpService.layer,
    GrpcService.layer,
    EventService.layer,
    KafkaConsumer.layer
  )
