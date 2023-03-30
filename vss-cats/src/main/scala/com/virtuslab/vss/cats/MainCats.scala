package com.virtuslab.vss.cats

import cats.effect.*
import cats.implicits.*
import com.virtuslab.vss.cats.modules.*
import cats.effect.std.Supervisor
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import com.virtuslab.vss.cats.http.CatsHttpServer
import com.virtuslab.vss.cats.grpc.CatsGrpcServer

object MainCats extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    val appResources = AppResources.make[IO]()
    val services = Services.make[IO](appResources.db)
    val httpApi = HttpApi.make[IO](services)
    val servers = for {
      _ <- CatsHttpServer[IO].newServer(httpApi.httpApp)
      _ <- CatsGrpcServer[IO].newServer(services)
    } yield ()
    servers.useForever

}
