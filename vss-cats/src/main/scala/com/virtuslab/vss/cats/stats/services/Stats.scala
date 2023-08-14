package com.virtuslab.vss.cats.stats.services

import upickle.default.*
import cats.implicits.*
import com.virtuslab.vss.common.Event
import fs2.kafka.KafkaProducer
import upickle.default.*
import cats.Monad
import cats.effect.std.AtomicCell
import cats.effect.IO

/**
 * Service responsible for handling events.
 */
sealed abstract class Stats {
  def getLatestEvents(amount: Int): IO[List[Event]]
  def addEvent(eventStr: String): IO[Unit]
}

object Stats {
  def make(
    eventsStore: AtomicCell[IO, Seq[Event]]
  ): Stats =
    new Stats {
      def getLatestEvents(amount: Int): IO[List[Event]] =
        eventsStore.get.map(_.take(amount).toList)

      def addEvent(eventStr: String): IO[Unit] =
        eventsStore.update(read[Event](eventStr) +: _)
    }
}
