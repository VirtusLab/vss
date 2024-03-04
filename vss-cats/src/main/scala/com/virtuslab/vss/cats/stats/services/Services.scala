package com.virtuslab.vss.cats.stats.services

import cats.effect.*
import cats.*
import doobie.*
import com.virtuslab.vss.cats.stats.services.*
import org.typelevel.log4cats.Logger
import fs2.kafka.KafkaProducer
import cats.effect.std.AtomicCell
import com.virtuslab.vss.common.*

sealed abstract class Services[F[_]](
  val stats: Stats[F]
)

object Services {
  def make[F[_]: Async: Logger](
    eventsStore: AtomicCell[F, Seq[Event]]
  ): Services[F] = {
    val stats = Stats.make[F](eventsStore)
    new Services[F](
      stats = stats
    ) {}
  }
}
