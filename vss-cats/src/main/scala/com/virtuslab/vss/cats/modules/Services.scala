package com.virtuslab.vss.cats.modules

import cats.effect.*
import cats.*
import doobie.*
import com.virtuslab.vss.cats.services.*
import org.typelevel.log4cats.Logger

sealed abstract class Services[F[_]](
  val passwords: Passwords[F]
)

object Services {
  def make[F[_]: Async: Logger](
    db: Resource[F, Transactor[F]]
  ): Services[F] = {
    new Services[F](
      passwords = Passwords.make[F](db)
    ) {}
  }
}

