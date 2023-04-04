package com.virtuslab.vss.cats.base.grpc

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all.*
import cats.*
import cats.effect.*
import org.typelevel.log4cats.*
import io.grpc.*
import com.virtuslab.vss.cats.base.grpc.services.*
import com.virtuslab.vss.cats.base.services.*

trait BaseGrpcServer[F[_]]:
  def newServer(services: Services[F]): Resource[F, Server]

object BaseGrpcServer:
  def apply[F[_]: BaseGrpcServer]: BaseGrpcServer[F] = summon

  val port: Int = 8081

  given forAsyncLogger[F[_]: Async: Logger]: BaseGrpcServer[F] =
    new BaseGrpcServer[F]:
      override def newServer(services: Services[F]): Resource[F, Server] =
        for {
          hashPasswordGrpcService <- HashPasswordGrpcService.make[F](services.passwords)
          checkPasswordGrpcService <- CheckPasswordGrpcService.make[F](services.passwords)
          server <- NettyServerBuilder
            .forPort(port)
            .addService(hashPasswordGrpcService)
            .addService(checkPasswordGrpcService)
            .resource[F]
            .evalMap(server => Async[F].delay(server.start()))
        } yield server
