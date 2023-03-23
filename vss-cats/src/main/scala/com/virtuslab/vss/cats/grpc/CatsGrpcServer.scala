package com.virtuslab.vss.cats.grpc

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all.*
import cats.*
import cats.effect.*
import org.typelevel.log4cats.*
import io.grpc.*
import com.virtuslab.vss.cats.grpc.services.*
import com.virtuslab.vss.cats.modules.*

trait CatsGrpcServer[F[_]]:
  def newServer(services: Services[F]): Resource[F, Server]

object CatsGrpcServer:
  def apply[F[_]: CatsGrpcServer]: CatsGrpcServer[F] = summon

  val port: Int = 8081

  given forAsyncLogger[F[_]: Async: Logger]: CatsGrpcServer[F] =
    new CatsGrpcServer[F]:
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
