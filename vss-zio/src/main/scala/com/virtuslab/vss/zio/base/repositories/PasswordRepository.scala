package com.virtuslab.vss.zio.base.repositories

import zio.*
import com.virtuslab.vss.common.*
import com.virtuslab.vss.zio.base.resources.Db.Schema.*
import io.getquill.*
import io.getquill.jdbczio.Quill.Postgres

trait PasswordRepository:
  def saveHash(hashedPassword: HashedPassword): RIO[Scope, Unit]
  def getHash(hashType: String, password: String): RIO[Scope, Option[HashedPassword]]
  def checkPwned(passwordHash: String): RIO[Scope, Boolean]

case class PasswordRepositoryImpl(db: Postgres[SnakeCase]) extends PasswordRepository:
  import db._
  override def saveHash(hashedPassword: HashedPassword): RIO[Scope, Unit] = run(
    sql"""insert into hashed_passwords (password, hash_type, password_hash) values (${lift(hashedPassword.password)}, ${lift(hashedPassword.hashType)}, ${lift(hashedPassword.hash)}) on conflict do nothing"""
      .as[Update[Unit]]
  ).either.unit

  override def getHash(hashType: String, password: String): RIO[Scope, Option[HashedPassword]] = for
    result <- run(sql"select * from hashed_passwords where hash_type = ${lift(hashType)} and password = ${lift(password)}".as[Query[HashedPasswords]])
  yield result.headOption.map(hp => HashedPassword(hp.hash_type, hp.password, hp.password_hash))

  override def checkPwned(passwordHash: String): RIO[Scope, Boolean] = for
    result <- run(sql"select * from hashed_passwords where password_hash = ${lift(passwordHash)}".as[Query[HashedPasswords]])
  yield result.nonEmpty

object PasswordRepository:
  val layer = ZLayer.fromFunction(PasswordRepositoryImpl.apply)
