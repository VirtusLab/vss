package com.virtuslab.vss.cats.base.services

import cats.effect.*
import cats.*
import doobie.*
import com.virtuslab.vss.cats.base.services.*
import org.typelevel.log4cats.Logger
import fs2.kafka.KafkaProducer
import natchez.Trace

sealed abstract class Services[F[_]](
  val passwords: Passwords[F],
  val events: Events[F]
)

object Services {
  def make[F[_] : Async : Logger : Trace](
    transactor: Transactor[F],
    kafka: KafkaProducer[F, String, String]
  ): Services[F] = {
    val events    = Events.make[F](kafka)
    val passwords = Passwords.make[F](transactor, events)
    new Services[F](
      passwords = passwords,
      events = events
    ) {}
  }
}
