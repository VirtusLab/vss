package com.virtuslab.vss.zio.base.grpc

import com.virtuslab.vss.proto.zio.password.*
import com.virtuslab.vss.common.*
import io.scalaland.chimney.dsl.*
import zio.*
import io.grpc.{Status, StatusException}
import scalapb.UnknownFieldSet

object GrpcOps:
  extension [R, E, A](zio: ZIO[R, E, A])
    def handleError: ZIO[R, StatusException, A] = zio.orElseFail(StatusException(Status.INTERNAL))

  extension (grpc: HashPasswordRequest) def toDomain: HashPassword = grpc.into[HashPassword].transform

  extension (dto: HashedPassword)
    def toGrpc: HashPasswordResponse = dto
      .into[HashPasswordResponse]
      .withFieldConst(_.unknownFields, UnknownFieldSet.empty)
      .transform

  extension (grpc: CheckPwnedRequest) def toDomain: CheckPwned = grpc.into[CheckPwned].transform

  extension (dto: CheckedPwned)
    def toGrpc: CheckPwnedResponse = dto
      .into[CheckPwnedResponse]
      .withFieldConst(_.unknownFields, UnknownFieldSet.empty)
      .transform
