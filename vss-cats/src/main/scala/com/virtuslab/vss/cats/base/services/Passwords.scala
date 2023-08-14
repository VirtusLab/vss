package com.virtuslab.vss.cats.base.services

import cats.*
import cats.implicits.*
import cats.effect.*
import doobie.*
import doobie.implicits.*
import com.virtuslab.vss.common.*
import org.apache.commons.codec.digest.DigestUtils
import org.typelevel.log4cats.Logger
import cats.data.*
import monocle.syntax.all.*
import natchez.Trace

/**
  * Service (algebra) for password hashing and checking if password was pwned.
  *
  * The logic related to password hashing and checking if password was pwned is
  * implemented in this service.
  *
  * Note that the declaration of `Passwords` does not have any typeclass
  * constraints. They are only present in the implementation.
  */
sealed abstract class Passwords[F[_]] {
  def checkPwned(checkPassword: CheckPwned): F[CheckedPwned]
  def hashPassword(hashPassword: HashPassword): F[HashedPassword]
}

object Passwords {
  /**
    * The implementation of `Passwords` service.
    *
    * @param transactor Transactor from `doobie` library, allows to execute SQL queries
    * @param events Service responsible for publishing events
    * @return
    */
  def make[F[_]: MonadThrow: MonadCancelThrow: Logger: Trace](
    transactor: Transactor[F],
    events: Events[F]
  ): Passwords[F] = new Passwords[F] {

    private def doCheckPwned(passwordHash: String): F[Boolean] = Trace[F].span("doCheckPwned") {
      sql"select hash_type from hashed_passwords where password_hash = $passwordHash"
        .query[String]
        .to[List]
        .transact(transactor)
        .map(_.nonEmpty)
    }

    override def checkPwned(checkPwned: CheckPwned): F[CheckedPwned] = Trace[F].span("checkPwned") {
      for {
        pwnd <- doCheckPwned(checkPwned.passwordHash).map(CheckedPwned.apply(checkPwned.passwordHash, _))
        _ <- Trace[F].put("passwordHash" -> checkPwned.passwordHash, "pwnd" -> pwnd.toString)
        _ <- events.publishEvent(Event.CheckedPwned(checkPwned.passwordHash))
      } yield pwnd
    }

    private def hashAlgorithm(hashType: String): F[(String => String)] = hashType match
      case "sha256" => Monad[F].pure(DigestUtils.sha256Hex)
      case _ =>
        MonadThrow[F].raiseError(new RuntimeException("Unsupported hash type"))
          <* Logger[F].info(s"Unsupported hash type: $hashType")

    private def saveHash(hashedPassword: HashedPassword): F[Unit] = Trace[F].span("saveHash") {
      sql"""|insert into hashed_passwords (hash_type, password_hash)
            | values (${hashedPassword.hashType}, ${hashedPassword.hash}) on conflict do nothing""".stripMargin
        .update
        .run
        .transact(transactor)
        .void
    }

    private def normalizeHashPassword(hashPassword: HashPassword): HashPassword =
      hashPassword.focus(_.hashType).modify(_.toLowerCase)

    override def hashPassword(_hashPassword: HashPassword): F[HashedPassword] = Trace[F].span("hashPassword") {
      val hashPassword = normalizeHashPassword(_hashPassword)
      for {
        _ <- Trace[F].put("hashType" -> hashPassword.hashType, "password" -> hashPassword.password)
        hashAlgorithm <- hashAlgorithm(hashPassword.hashType)
        hash = hashAlgorithm(hashPassword.password)
        hashedPassword = HashedPassword(hashPassword.hashType, hashPassword.password, hash)
        _ <- Trace[F].put("hash" -> hashedPassword.hash)
        _ <- saveHash(hashedPassword)
        _ <- events.publishEvent(Event.HashedPassword(hashPassword.password, hashPassword.hashType))
      } yield hashedPassword
    }

  }
}
