package com.virtuslab.vss.cats.base.grpc.services

import cats.effect.*
import cats.*
import cats.syntax.all.*
import io.grpc.*
import fs2.grpc.syntax.all.*
import com.virtuslab.vss.proto.cats.password.*
import com.virtuslab.vss.cats.base.services.*
import com.virtuslab.vss.common.*

object HashPasswordGrpcService {
  def make[F[_]: Async: Monad](passwords: Passwords[F]): Resource[F, ServerServiceDefinition] =
    HashPasswordServiceFs2Grpc.bindServiceResource[F](new HashPasswordServiceFs2Grpc {
      override def hashPassword(request: HashPasswordMessage, ctx: Metadata): F[HashedPasswordMessage] =
        passwords.hashPassword(
          HashPassword(request.hashType, request.password)
        ).map { hashedPassword =>
          HashedPasswordMessage(hashedPassword.hashType, hashedPassword.password, hashedPassword.hash)
        }
    })
}
