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

sealed abstract class Passwords[F[_]] {
  def checkPwned(checkPassword: CheckPwned): F[CheckedPwned]
  def hashPassword(hashPassword: HashPassword): F[HashedPassword]
}

object Passwords {
  def make[F[_]: MonadThrow: MonadCancelThrow: Logger](
    transactor: Transactor[F],
    events: Events[F]
  ): Passwords[F] = new Passwords[F] {

    private def doCheckPwned(email: String): F[Int] =
      sql"select breaches_no from pwned where email = $email"
        .query[Int]
        .option
        .transact(transactor)
        .map(_.getOrElse(0))

    override def checkPwned(checkPwned: CheckPwned): F[CheckedPwned] =
      doCheckPwned(checkPwned.email).map(CheckedPwned.apply(checkPwned.email, _))
        <* events.publishEvent(Event.CheckedPwned(checkPwned.email))

    private def hashAlgorithm(hashType: String): F[(String => String)] = hashType match
      case "sha256" => Monad[F].pure(DigestUtils.sha256Hex)
      case _ =>
        MonadThrow[F].raiseError(new RuntimeException("Unsupported hash type"))
          <* Logger[F].info(s"Unsupported hash type: $hashType")

    private def saveHash(hashedPassword: HashedPassword): F[Unit] =
      sql"""|insert into hashed_passwords (password, hash_type, password_hash)
            | values (${hashedPassword.password}, ${hashedPassword.hashType}, ${hashedPassword.hash}) on conflict do nothing""".stripMargin
        .update
        .run
        .transact(transactor)
        .void

    private def getHash(hashType: String, password: String): F[Option[HashedPassword]] =
      sql"select hash_type, password, password_hash from hashed_passwords where hash_type = $hashType and password = $password"
        .query[HashedPassword]
        .option
        .transact(transactor)

    private def normalizeHashPassword(hashPassword: HashPassword): HashPassword =
      hashPassword.focus(_.hashType).modify(_.toLowerCase)

    override def hashPassword(_hashPassword: HashPassword): F[HashedPassword] =
      val hashPassword = normalizeHashPassword(_hashPassword)
      for {
        maybeHashedPassword <- getHash(hashPassword.hashType, hashPassword.password)
        hashedPassword <- maybeHashedPassword match {
          case Some(hashedPassword) =>
            Monad[F].pure(hashedPassword)
              <* Logger[F].info(s"Using cached hash for hashType = ${hashPassword.hashType} and password = ${hashPassword.password}}")
          case None =>
            for {
              hashAlgorithm <- hashAlgorithm(hashPassword.hashType)
              hash = hashAlgorithm(hashPassword.password)
              hashedPassword = HashedPassword(hashPassword.hashType, hashPassword.password, hash)
            } yield hashedPassword
        }
        _ <- events.publishEvent(Event.HashedPassword(hashPassword.password, hashPassword.hashType))
        _ <- saveHash(hashedPassword)
      } yield hashedPassword

  }
}
