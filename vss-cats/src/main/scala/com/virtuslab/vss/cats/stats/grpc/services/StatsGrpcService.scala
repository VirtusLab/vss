package com.virtuslab.vss.cats.stats.grpc.services

import cats.effect.*
import cats.*
import cats.syntax.all.*
import io.grpc.*
import fs2.grpc.syntax.all.*
import com.virtuslab.vss.proto.cats.password.*
import com.virtuslab.vss.common.*
import com.virtuslab.vss.cats.stats.services.*

object StatsGrpcService {
  def make[F[_]: Async: Monad](stats: Stats[F]): Resource[F, ServerServiceDefinition] =
    StatsServiceFs2Grpc.bindServiceResource[F](new StatsServiceFs2Grpc {
      override def getAllEvents(request: EmptyRequest, ctx: Metadata): F[AllEvents] =
        stats.getAllEvents()
          .map(_.map {
            case event @ Event.CheckedPwned(email) =>
              EventResponse(eventType = event.getClass().getCanonicalName(), email = Some(email))
            case event @ Event.HashedPassword(password, hashType) =>
              EventResponse(eventType = event.getClass().getCanonicalName(), password = Some(password), hashType = Some(hashType))
          })
          .map(AllEvents(_))
          
    })
}