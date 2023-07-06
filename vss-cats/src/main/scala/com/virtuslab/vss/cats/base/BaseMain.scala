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

/**
  * This object is responsible for starting the base application. It loads the
  * configuration, creates resources and starts the servers.
  *
  * Most top-level classes are instantiated with the `IO` effect type. All the
  * services and resources are instantiated with the `TIO` effect type.
  */
object BaseMain {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  /** Span[IO] => IO[A] */
  type TIO[A] = Kleisli[IO, Span[IO], A]

  def fromTraceK(s: Span[IO]): FunctionK[TIO, IO] = new FunctionK[TIO, IO] {
    def apply[A](l: TIO[A]): IO[A] = l.run(s)
  }

  def toTraceK: FunctionK[IO, TIO] = new FunctionK[IO, TIO] {
    def apply[A](l: IO[A]): TIO[A] = Kleisli.liftF(l)
  }

  /**
    * This is the main entry point of the application. It instantiates all the
    * services, resources and servers.
    *
    * The `for` loop runs in a `Resource` context. This means that all the
    * resources are released after the application is stopped.
    *
    * All resources are only started in the `Resource` context. This means that
    * starting the grpc server does not have to wait until the http server
    * stops.
    */
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
