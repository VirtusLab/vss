package com.virtuslab.vss.cats.base.services

import cats.implicits.*
import com.virtuslab.vss.common.Event
import fs2.kafka.KafkaProducer
import cats.Monad
import com.github.plokhotnyuk.jsoniter_scala.core.*

/**
  * Service responsible for publishing events to Kafka.
  */
sealed abstract class Events[F[_]] {
  def publishEvent(event: Event): F[Unit]
}

object Events {
  def make[F[_] : Monad](
    kafka: KafkaProducer[F, String, String]
  ): Events[F] =
    new Events[F] {
      override def publishEvent(event: Event): F[Unit] =
        kafka.produceOne("events", "event", writeToString(event)).flatten.void
    }
}
