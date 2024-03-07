package com.virtuslab.vss.cats.stats.services

import com.github.plokhotnyuk.jsoniter_scala.core.*
import cats.implicits.*
import com.virtuslab.vss.common.Event
import fs2.kafka.KafkaProducer
import cats.Monad
import cats.effect.std.AtomicCell

/**
  * Service responsible for handling events.
  */
sealed abstract class Stats[F[_]] {
  def getLatestEvents(amount: Int): F[List[Event]]
  def addEvent(eventStr: String): F[Unit]
}

object Stats {
  def make[F[_] : Monad](
    eventsStore: AtomicCell[F, Seq[Event]]
  ): Stats[F] =
    new Stats[F] {
      def getLatestEvents(amount: Int): F[List[Event]] =
        eventsStore.get.map(_.take(amount).toList)

      def addEvent(eventStr: String): F[Unit] =
        eventsStore.update(readFromString[Event](eventStr) +: _)
    }
}
