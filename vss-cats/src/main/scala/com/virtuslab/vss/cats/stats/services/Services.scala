package com.virtuslab.vss.cats.stats.services

import cats.effect.*
import cats.*
import doobie.*
import com.virtuslab.vss.cats.stats.services.*
import org.typelevel.log4cats.Logger
import fs2.kafka.KafkaProducer
import cats.effect.std.AtomicCell
import com.virtuslab.vss.common.*
import cats.effect.IO

sealed abstract class Services(
  val stats: Stats
)

object Services {
  def make(
    eventsStore: AtomicCell[IO, Seq[Event]]
  ): Services = {
    val stats = Stats.make(eventsStore)
    new Services(
      stats = stats
    ) {}
  }
}
