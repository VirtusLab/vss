package com.virtuslab.vss.zio.stats.grpc

import zio.*
import io.grpc.{Status, StatusException}
import com.virtuslab.vss.proto.zio.password.{HashPasswordResponse, ListEventsRequest}
import com.virtuslab.vss.proto.zio.password.{Event => GrpcEvent}
import com.virtuslab.vss.common.{Event => ScalaEvent}

object GrpcOps:
  extension [R, E, A](zio: ZIO[R, E, A])
    def handleError: ZIO[R, StatusException, A] =
      zio.orElseFail(StatusException(Status.INTERNAL))

  extension (dto: ScalaEvent)
    def toGrpc: GrpcEvent = dto match
      case event @ ScalaEvent.HashedPassword(password, hashType) =>
        GrpcEvent(eventType = event.getClass().getCanonicalName(), password = Some(password), hashType = Some(hashType))
      case event @ ScalaEvent.CheckedPwned(passwordHash) =>
        GrpcEvent(eventType = event.getClass().getCanonicalName(), passwordHash = Some(passwordHash))
