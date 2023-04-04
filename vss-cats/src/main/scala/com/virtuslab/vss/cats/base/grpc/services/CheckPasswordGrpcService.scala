package com.virtuslab.vss.cats.base.grpc.services

import cats.effect.*
import cats.*
import cats.syntax.all.*
import io.grpc.*
import fs2.grpc.syntax.all.*
import com.virtuslab.vss.proto.cats.password.*
import com.virtuslab.vss.cats.base.services.*
import com.virtuslab.vss.common.*

object CheckPasswordGrpcService {
  def make[F[_]: Async: Monad](passwords: Passwords[F]): Resource[F, ServerServiceDefinition] =
    CheckPasswordServiceFs2Grpc.bindServiceResource[F](new CheckPasswordServiceFs2Grpc {
      override def checkPassword(request: CheckPasswordMessage, ctx: Metadata): F[CheckedPasswordMessage] =
        passwords.checkPassword(
          CheckPassword(request.password)
        ).map { checkedPassword =>
          CheckedPasswordMessage(checkedPassword.pwned)
        }
    })
}
