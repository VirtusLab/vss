package com.virtuslab.vss.zio.base.resources

import com.virtuslab.vss.zio.base.config.DbConfig
import doobie.hikari.HikariTransactor
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig
import zio.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import zio.interop.catz.*

trait Postgres:
  def getTransactor: ZIO[Any & Scope, Throwable, Transactor[Task]]

case class PostgresImpl() extends Postgres:

  override def getTransactor: ZIO[Any & Scope, Throwable, Transactor[Task]] = for
    config <- ZIO.config(DbConfig.config)
    ec <- ZIO.executor.map(_.asExecutionContext)
    tx <- HikariTransactor
      .newHikariTransactor[Task](
        "org.postgresql.Driver",
        s"jdbc:postgresql://${config.postgresHost}:${config.postgresPort}/${config.dbName}",
        config.postgresUser,
        config.postgresPassword,
        ec
      )
      .toScopedZIO
  yield tx

object DbConnection:
  val layer: ULayer[PostgresImpl] = ZLayer.succeed(PostgresImpl())
