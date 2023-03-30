package com.virtuslab.vss.cats.services

import cats.*
import cats.implicits.*
import cats.effect.*
import doobie.*
import doobie.implicits.*
import com.virtuslab.vss.common.*
import org.apache.commons.codec.digest.DigestUtils
import org.typelevel.log4cats.Logger
import cats.data.*

sealed abstract class Passwords[F[_]] {
  def checkPassword(checkPassword: CheckPassword): F[CheckedPassword]
  def hashPassword(hashPassword: HashPassword): F[HashedPassword]
}

object Passwords {
  def make[F[_]: MonadThrow: MonadCancelThrow: Logger](
    db: Resource[F, Transactor[F]]
  ): Passwords[F] = new Passwords[F] {

    private def makeAllHashes(password: String): NonEmptyList[(String, String)] =
      NonEmptyList.of(
        "SHA256" -> DigestUtils.sha256Hex(password)
      )

    private def checkHashes(hashes: NonEmptyList[(String, String)]): F[CheckedPassword] =
      val values = hashes.map { (hashType, hash) =>
        hash
      }
      db.use { transactor =>
        (fr"select count(*) from hashed_passwords where password_hash in " ++ Fragments.parentheses(Fragments.values(values)))
          .query[Int]
          .unique
          .transact(transactor)
      }.map { (count: Int) =>
        CheckedPassword(count > 0)
      }

    override def checkPassword(checkPassword: CheckPassword): F[CheckedPassword] =
      val allHashes = makeAllHashes(checkPassword.password)
      checkHashes(allHashes)

    private def hashAlgorithm(hashType: String): F[(String => String)] = hashType.toLowerCase match
      case "sha256" => Monad[F].pure(DigestUtils.sha256Hex)
      case _ =>
        MonadThrow[F].raiseError(new RuntimeException("Unsupported hash type"))
          <* Logger[F].info(s"Wrong hash type: $hashType")

    private def saveHash(hashedPassword: HashedPassword): F[Unit] =
      db.use { transactor =>
        sql"insert into hashed_passwords (hash_type, password_hash) values (${hashedPassword.hashType}, ${hashedPassword.hash})"
          .update
          .run
          .transact(transactor)
          .void
      }

    override def hashPassword(hashPassword: HashPassword): F[HashedPassword] =
      for {
        hashAlgorithm <- hashAlgorithm(hashPassword.hashType)
        hash = hashAlgorithm(hashPassword.password)
        hashedPassword = HashedPassword(hashPassword.hashType, hashPassword.password, hash)
        _ <- saveHash(hashedPassword)
      } yield hashedPassword

  }
}
