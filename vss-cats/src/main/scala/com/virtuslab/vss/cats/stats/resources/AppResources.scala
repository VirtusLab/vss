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

sealed abstract class AppResources[F[_]](
  val kafkaConsumer: KafkaConsumer[F, String, String],
  val eventsStore: AtomicCell[F, Seq[Event]]
)

object AppResources {

  def make[F[_]: Sync: Async: Logger](): Resource[F, AppResources[F]] = {

    val kafkaSettings =  
      ConsumerSettings[F, String, String]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers("localhost:9092")
        .withGroupId("stats")
  

    def kafkaResource(): Resource[F, KafkaConsumer[F, String, String]] =
      KafkaConsumer.resource(kafkaSettings)
    
    for {
      kafka <- kafkaResource()
      eventsStore <- Resource.eval(AtomicCell[F].of[Seq[Event]](Seq.empty))
    } yield new AppResources(kafka, eventsStore) {}
  }
}