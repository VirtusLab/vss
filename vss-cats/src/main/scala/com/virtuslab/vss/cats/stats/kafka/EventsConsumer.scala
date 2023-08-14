package com.virtuslab.vss.cats.stats.kafka

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import fs2.kafka.KafkaConsumer
import com.virtuslab.vss.cats.stats.services.Services
import cats.effect.IO

object EventsConsumer {
  def runConsumer(
    kafkaConsumer: KafkaConsumer[IO, String, String],
    services: Services
  )(using fs2.Compiler[IO, IO]): IO[Unit] = for {
    _ <- kafkaConsumer.subscribeTo("events")
    _ <- kafkaConsumer.stream.evalTap { record =>
      services.stats.addEvent(record.record.value)
    }.compile.drain
  } yield ()
}
