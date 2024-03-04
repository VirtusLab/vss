package com.virtuslab.vss.cats.stats.resources

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor.*
import doobie.hikari.HikariTransactor
import org.typelevel.log4cats.Logger
import fs2.kafka.*
import cats.effect.std.AtomicCell
import _root_.com.virtuslab.vss.common.Event
import _root_.com.virtuslab.vss.cats.stats.config.StatsAppConfig

sealed abstract class AppResources[F[_]](
  val kafkaConsumer: KafkaConsumer[F, String, String],
  val eventsStore: AtomicCell[F, Seq[Event]]
)

object AppResources {

  /**
    * Creates a new instance of `AppResources` for a given effect type `F`.
    *
    * Instantiates a Kafka consumer and an in-memory store for events.
    */
  def make[F[_] : Sync : Async : Logger](appConfig: StatsAppConfig): Resource[F, AppResources[F]] = {

    val kafkaSettings =
      ConsumerSettings[F, String, String]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers(s"${appConfig.kafkaHost}:${appConfig.kafkaPort}")
        .withGroupId("stats")

    def kafkaResource(): Resource[F, KafkaConsumer[F, String, String]] =
      KafkaConsumer.resource(kafkaSettings)

    (
      kafkaResource(),
      Resource.eval(AtomicCell[F].of[Seq[Event]](Seq.empty))
    ).parMapN { (kafka, eventsStore) =>
      new AppResources(kafka, eventsStore) {}
    }
  }
}
