package com.virtuslab.vss.cats.base

import cats.data.*
import cats.effect.*
import cats.implicits.*
import cats.arrow.FunctionK
import cats.effect.std.Supervisor
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger
import com.virtuslab.vss.cats.base.resources.*
import com.virtuslab.vss.cats.base.http.BaseHttpServer
import com.virtuslab.vss.cats.base.grpc.BaseGrpcServer
import com.virtuslab.vss.cats.base.services.Services
import com.virtuslab.vss.cats.base.http.HttpApi
import com.virtuslab.vss.cats.base.config.Config
import natchez.http4s.implicits.*
import natchez.{ Span, EntryPoint }

object BaseMain {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  type TIO[A] = Kleisli[IO, Span[IO], A]

  def fromTraceK(s: Span[IO]): FunctionK[TIO, IO] = new FunctionK[TIO, IO] {
    def apply[A](l: TIO[A]): IO[A] = l.run(s)
  }

  def unliftTraceK: FunctionK[IO, TIO] = new FunctionK[IO, TIO] {
    def apply[A](l: IO[A]): TIO[A] = Kleisli.liftF(l)
  }

  def run: IO[Unit] = {
    for {
      appConfig <- Resource.eval(Config.load[IO]())
      ep <- AppResources.makeEntryPoint[IO](appConfig)
      rootSpan <- ep.root("root")
      res <- AppResources.make[TIO](appConfig).mapK(fromTraceK(rootSpan))
      services = Services.make[TIO](res.db, res.kafka)
      httpApi = ep.liftT(HttpApi.make(services))
      _ <- BaseHttpServer[IO].newServer(appConfig, httpApi.orNotFound)
      _ <- BaseGrpcServer[TIO].newServer(appConfig, services).mapK(fromTraceK(rootSpan))
    } yield ()
  }.useForever
}
