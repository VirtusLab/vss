package com.virtuslab.vss.cats.stats.services

import upickle.default.*
import cats.implicits.*
import com.virtuslab.vss.common.Event
import fs2.kafka.KafkaProducer
import upickle.default.*
import cats.Monad
import cats.effect.std.AtomicCell

sealed abstract class Stats[F[_]] {
  def getAllEvents(): F[List[Event]]
  def addEvent(eventStr: String): F[Unit]
}

object Stats {
  def make[F[_]: Monad](
    eventsStore: AtomicCell[F, Seq[Event]]
  ): Stats[F] =
    new Stats[F] {

      def getAllEvents(): F[List[Event]] =
        eventsStore.get.map(_.toList)

      def addEvent(eventStr: String): F[Unit] =
        eventsStore.update(_ :+ read[Event](eventStr))

    }
}
