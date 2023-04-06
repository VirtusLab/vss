package com.virtuslab.vss.cats.stats.grpc

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all.*
import cats.*
import cats.effect.*
import org.typelevel.log4cats.*
import io.grpc.*
import com.virtuslab.vss.cats.stats.services.*
import com.virtuslab.vss.cats.stats.grpc.services.*

trait StatsGrpcServer[F[_]]:
  def newServer(services: Services[F]): Resource[F, Server]

object StatsGrpcServer:
  def apply[F[_]: StatsGrpcServer]: StatsGrpcServer[F] = summon

  val port: Int = 8181

  given forAsyncLogger[F[_]: Async: Logger]: StatsGrpcServer[F] =
    new StatsGrpcServer[F]:
      override def newServer(services: Services[F]): Resource[F, Server] =
        for {
          statsGrpcService <- StatsGrpcService.make[F](services.stats)
          server <- NettyServerBuilder
            .forPort(port)
            .addService(statsGrpcService)
            .resource[F]
            .evalMap(server => Async[F].delay(server.start()))
        } yield server