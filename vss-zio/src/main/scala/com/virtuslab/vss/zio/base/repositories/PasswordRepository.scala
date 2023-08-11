package com.virtuslab.vss.zio.base.repositories

import zio.*
import doobie.*
import doobie.implicits.*
import zio.interop.catz.*
import com.virtuslab.vss.common.*
import com.virtuslab.vss.zio.base.resources.Postgres

trait PasswordRepository:
  def saveHash(hashedPassword: HashedPassword): RIO[Scope, Unit]
  def getHash(hashType: String, password: String): RIO[Scope, Option[HashedPassword]]
  def checkPwned(passwordHash: String): RIO[Scope, Boolean]

case class PasswordRepositoryImpl(db: Postgres) extends PasswordRepository:

  override def saveHash(hashedPassword: HashedPassword): RIO[Scope, Unit] = for
    tx <- db.getTransactor
    result <- sql"""|insert into hashed_passwords (password, hash_type, password_hash)
          | values (${hashedPassword.password}, ${hashedPassword.hashType}, ${hashedPassword.hash}) on conflict do nothing""".stripMargin.update.run
      .transact(tx)
  yield ()

  override def getHash(hashType: String, password: String): RIO[Scope, Option[HashedPassword]] = for
    tx <- db.getTransactor
    result <-
      sql"select hash_type, password, password_hash from hashed_passwords where hash_type = $hashType and password = $password"
        .query[HashedPassword]
        .option
        .transact(tx)
  yield result

  override def checkPwned(passwordHash: String): RIO[Scope, Boolean] = for
    tx <- db.getTransactor
    result <- sql"select hash_type from hashed_passwords where password_hash = $passwordHash"
      .query[String]
      .to[List]
      .transact(tx)
      .map(_.nonEmpty)
  yield result

object PasswordRepository:
  val layer = ZLayer.fromFunction(PasswordRepositoryImpl.apply)
