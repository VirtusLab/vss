package com.virtuslab.vss.zio.stats

import zio.*
import com.virtuslab.vss.zio.stats.http.HttpService
import com.virtuslab.vss.zio.stats.grpc.GrpcService
import com.virtuslab.vss.zio.stats.services.EventService
import com.virtuslab.vss.zio.stats.resources.KafkaConsumer

object StatsMain:
  private def app: RIO[Any & HttpService & GrpcService & KafkaConsumer, Unit] = for
    http          <- ZIO.service[HttpService]
    grpc          <- ZIO.service[GrpcService]
    kafka         <- ZIO.service[KafkaConsumer]
    httpServer    <- http.serve().fork
    grpcServer    <- grpc.serve().fork
    kafkaConsumer <- kafka.consume.runDrain.fork
    _ <- httpServer
      .zip(grpcServer)
      .zip(kafkaConsumer)
      .join
  yield ()

  def run: Task[Unit] = app.provide(
    HttpService.layer,
    GrpcService.layer,
    EventService.layer,
    KafkaConsumer.layer
  )
