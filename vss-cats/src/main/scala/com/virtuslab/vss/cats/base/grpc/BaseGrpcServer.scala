package com.virtuslab.vss.cats.base.grpc

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all.*
import cats.*
import cats.effect.*
import org.typelevel.log4cats.*
import io.grpc.*
import com.virtuslab.vss.cats.base.grpc.services.*
import com.virtuslab.vss.cats.base.services.*
import com.virtuslab.vss.cats.base.config.BaseAppConfig
import java.net.InetSocketAddress
import com.google.common.net.InetAddresses
import natchez.{ Span, Trace }

object BaseGrpcServer:
  def make[F[_]: Async: Logger: Trace](
    appConfig: BaseAppConfig,
    services: Services[F],
  ): Resource[F, Server] =
    for {
      hashPasswordGrpcService <- HashPasswordGrpcService.make[F](services.passwords)
      checkPasswordGrpcService <- CheckPasswordGrpcService.make[F](services.passwords)
      server <- NettyServerBuilder
        .forAddress(new InetSocketAddress(InetAddresses.forString(appConfig.grpcHost), appConfig.grpcPort))
        .addService(hashPasswordGrpcService)
        .addService(checkPasswordGrpcService)
        .resource[F]
        .evalMap(server => Async[F].delay(server.start()))
    } yield server
