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
  def make[F[_] : Async : Monad](passwords: Passwords[F]): Resource[F, ServerServiceDefinition] =
    PwnedServiceFs2Grpc.bindServiceResource[F](new PwnedServiceFs2Grpc {
      override def checkPwned(request: CheckPwnedRequest, ctx: Metadata): F[CheckPwnedResponse] =
        passwords
          .checkPwned(
            CheckPwned(request.passwordHash)
          )
          .map { checkedPassword =>
            CheckPwnedResponse(request.passwordHash, checkedPassword.pwned)
          }
    })
}
