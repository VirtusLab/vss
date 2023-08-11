package com.virtuslab.vss.zio.stats.grpc

import zio.*
import scalapb.zio_grpc.*
import io.grpc.{ServerBuilder, Status, StatusException}
import io.grpc.protobuf.services.ProtoReflectionService
import com.virtuslab.vss.proto.zio.password.*
import com.virtuslab.vss.zio.stats.config.GrpcConfig
import com.virtuslab.vss.zio.stats.services.EventService
import GrpcOps.*

trait GrpcService:
  def serve(): Task[Unit]

case class GrpcServiceImpl(eventService: EventService) extends GrpcService:

  private case class StatsService() extends ZioPassword.StatsService:
    override def listEvents(request: ListEventsRequest): IO[StatusException, ListEventsReponse] =
      eventService.listEvents()
        .handleError
        .map(result => ListEventsReponse(result.map(_.toGrpc)))

  def serve(): Task[Unit] = ZIO.scoped {
    for
      config <- ZIO.config(GrpcConfig.config)
      serverLayer = ServerLayer.fromServiceList(
        ServerBuilder
          .forPort(config.port)
          .addService(ProtoReflectionService.newInstance()),
        ServiceList.add(StatsService())
      )
      _ <- serverLayer.build *> ZIO.never
    yield ()
  }

object GrpcService:
  val layer: URLayer[EventService, GrpcService] = ZLayer.fromFunction(GrpcServiceImpl.apply)
