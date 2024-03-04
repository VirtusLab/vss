package com.virtuslab.vss.cats.stats.grpc

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all.*
import cats.*
import cats.effect.*
import org.typelevel.log4cats.*
import io.grpc.*
import com.virtuslab.vss.cats.stats.services.*
import com.virtuslab.vss.cats.stats.grpc.services.*
import com.virtuslab.vss.cats.stats.config.StatsAppConfig
import java.net.InetSocketAddress
import com.google.common.net.InetAddresses

object StatsGrpcServer:
  def make[F[_] : Async](appConfig: StatsAppConfig, services: Services[F]): Resource[F, Server] =
    for {
      statsGrpcService <- StatsGrpcService.make[F](services.stats)
      server <- NettyServerBuilder
        .forAddress(new InetSocketAddress(InetAddresses.forString(appConfig.grpcHost), appConfig.grpcPort))
        .addService(statsGrpcService)
        .resource[F]
        .evalMap(server => Async[F].delay(server.start()))
    } yield server
