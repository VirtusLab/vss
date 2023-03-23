package com.virtuslab.vss.cats.modules

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor.*
import doobie.hikari.HikariTransactor
import org.typelevel.log4cats.Logger

sealed abstract class AppResources[F[_]](
  val postgres: Resource[F, Transactor[F]]
)

object AppResources {

  def make[F[_]: Sync: Async: Logger](): AppResources[F] = {
    def checkPostgresConnection(transactor: Transactor[F]): F[Unit] =
      sql"select version();".query[String].unique.transact(transactor).flatMap { v =>
        Logger[F].info(s"Connected to Postgres $v")
      }

    def postgreSqlResource(): Resource[F, Transactor[F]] =
      for {
        ec <- ExecutionContexts.cachedThreadPool
        xa <- HikariTransactor.newHikariTransactor[F](
          "org.postgresql.Driver",
          "jdbc:postgresql://localhost:5432/vss",
          "postgres",
          "postgres",
          ec
        )
      } yield xa
    
    val postgres = postgreSqlResource().evalTap(checkPostgresConnection)
    new AppResources(postgres) {}
  }
}