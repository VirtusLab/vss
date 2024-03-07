package com.virtuslab.vss.zio.base.repositories

import zio.*
import com.virtuslab.vss.common.*
import com.virtuslab.vss.zio.base.resources.Db.Schema.*
import io.getquill.*
import io.getquill.jdbczio.Quill.Postgres
import org.checkerframework.checker.units.qual.h

case class HashedPasswordRow(hashType: String, passwordHash: String)

trait PasswordRepository:
  def saveHash(hashedPassword: HashedPassword): RIO[Scope, Unit]
  def checkPwned(passwordHash: String): RIO[Scope, Long]

case class PasswordRepositoryImpl(db: Postgres[SnakeCase]) extends PasswordRepository:
  import db.*

  inline def schema = quote(querySchema[HashedPasswordRow]("hashed_passwords"))

  override def saveHash(hashedPassword: HashedPassword): RIO[Scope, Unit] = run(
    // sql"""INSERT INTO hashed_passwords (hash_type, password_hash)
    //       VALUES (${lift(hashedPassword.hashType)}, ${lift(hashedPassword.hash)})
    //       ON CONFLICT DO NOTHING""".as[Update[Unit]]
    schema.insertValue(lift(HashedPasswordRow(hashedPassword.hashType, hashedPassword.hash)))
  ).unit

  override def checkPwned(passwordHash: String): RIO[Scope, Long] = run(
    // sql"""SELECT COUNT(*) FROM hashed_passwords
    //       WHERE password_hash = ${lift(passwordHash)}"""
    //   .as[Query[Int]]
    schema.filter(_.passwordHash == lift(passwordHash)).size
  )

object PasswordRepository:
  val layer = ZLayer.fromFunction(PasswordRepositoryImpl.apply)
