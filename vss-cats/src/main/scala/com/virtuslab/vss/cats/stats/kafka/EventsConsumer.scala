package com.virtuslab.vss.cats.stats.kafka

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import fs2.kafka.KafkaConsumer
import com.virtuslab.vss.cats.stats.services.Services

object EventsConsumer {
  def runConsumer[F[_]: Monad](
    kafkaConsumer: KafkaConsumer[F, String, String],
    services: Services[F]
  )(using fs2.Compiler[F, F]): F[Unit] = for {
    _ <- kafkaConsumer.subscribeTo("events")
    _ <- kafkaConsumer.stream.evalTap { record =>
      services.stats.addEvent(record.record.value)
    }.compile.drain
  } yield ()
}
