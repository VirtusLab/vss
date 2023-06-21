package com.virtuslab.vss.cats.stats.kafka

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import fs2.kafka.KafkaConsumer
import com.virtuslab.vss.cats.stats.services.Services

trait EventsConsumer[F[_]] {
  def runConsumer(
    kafkaConsumer: KafkaConsumer[F, String, String],
    services: Services[F]
  ): F[Unit]
}

object EventsConsumer {
  def apply[F[_]](using ev: EventsConsumer[F]): EventsConsumer[F] = summon

  given [F[_]: Monad](using fs2.Compiler[F, F]): EventsConsumer[F] with {
    def runConsumer(
      kafkaConsumer: KafkaConsumer[F, String, String],
      services: Services[F]
    ): F[Unit] = for {
      _ <- kafkaConsumer.subscribeTo("events")
      _ <- kafkaConsumer.stream.evalTap { record =>
        services.stats.addEvent(record.record.value)
      }.compile.drain
    } yield ()
  }
}
