package com.virtuslab.vss.zio.base.resources

import com.virtuslab.vss.zio.base.config.DbConfig
import com.zaxxer.hikari.HikariDataSource
import io.getquill.*
import io.getquill.jdbczio.Quill.Postgres
import zio.*
import java.util.UUID

object Db:

  private val dataSourceLayer: ULayer[HikariDataSource] = ZLayer.scoped {
    ZIO.acquireRelease(getDataSource.orDie)(ds => ZIO.attempt(ds.close()).orDie)
  }

  val layer: ULayer[Postgres[SnakeCase]] =
    dataSourceLayer >>> Postgres.fromNamingStrategy(SnakeCase)

  def getDataSource: ZIO[Any, Throwable, HikariDataSource] = for
    config <- ZIO.config(DbConfig.config)
    ds <- ZIO.attempt {
      val ds = new HikariDataSource()
      ds.setJdbcUrl(s"jdbc:postgresql://${config.postgresHost}:${config.postgresPort}/${config.dbName}")
      ds.setUsername(config.postgresUser)
      ds.setPassword(config.postgresPassword)
      ds
    }
  yield ds

  object Schema:
    case class HashedPasswords(uuid: UUID, password: String, hash_type: String, password_hash: String)