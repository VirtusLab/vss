package com.virtuslab.vss.cats.base.resources

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor.*
import doobie.hikari.HikariTransactor
import org.typelevel.log4cats.Logger
import fs2.kafka.ProducerSettings
import fs2.kafka.KafkaProducer
import com.virtuslab.vss.cats.base.config.BaseAppConfig

sealed abstract class AppResources[F[_]](
  val db: Transactor[F],
  val kafka: KafkaProducer[F, String, String]
)

object AppResources {

  def make[F[_]: Sync: Async: Logger](appConfig: BaseAppConfig): Resource[F, AppResources[F]] = {
    def checkDbConnection(transactor: Transactor[F]): F[Unit] =
      sql"select version();".query[String].unique.transact(transactor).flatMap { v =>
        Logger[F].info(s"Connected to DB $v")
      }

    def postgreSqlResource(): Resource[F, Transactor[F]] =
      for {
        ec <- ExecutionContexts.cachedThreadPool
        transactor <- HikariTransactor.newHikariTransactor[F](
          "org.postgresql.Driver",
          s"jdbc:postgresql://${appConfig.postgresHost}:${appConfig.postgresPort}/${appConfig.dbName}",
          appConfig.postgresUser,
          appConfig.postgresPassword,
          ec
        )
      } yield transactor

    val producerSettings =  
      ProducerSettings[F, String, String]  
        .withBootstrapServers(s"${appConfig.kafkaHost}:${appConfig.kafkaPort}")  
        .withProperty("topic.creation.enable", "true")

    def kafkaResource(): Resource[F, KafkaProducer[F, String, String]] =
      KafkaProducer.resource(producerSettings)
    
    (
      postgreSqlResource().evalTap(checkDbConnection),
      kafkaResource()
    ).parMapN { (db, kafka) =>
      new AppResources(db, kafka) {}
    }
  }
}