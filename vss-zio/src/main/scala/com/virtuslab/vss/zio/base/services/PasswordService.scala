package com.virtuslab.vss.zio.base.services

import zio.*
import com.virtuslab.vss.common.{CheckPwned, CheckedPwned, Event, HashPassword, HashedPassword}
import org.apache.commons.codec.digest.DigestUtils
import zio.telemetry.opentracing.OpenTracing
import com.virtuslab.vss.zio.base.repositories.PasswordRepository
import com.virtuslab.vss.zio.base.resources.TracingOps.*
import com.virtuslab.vss.zio.base.resources.*
import com.github.plokhotnyuk.jsoniter_scala.core.*

trait PasswordService:
  def checkPwned(checkPassword: CheckPwned): Task[CheckedPwned]
  def hashPassword(hashPassword: HashPassword): Task[HashedPassword]

case class PasswordServiceImpl(repository: PasswordRepository, kafka: KafkaProducer, tracing: OpenTracing)
  extends PasswordService:

  override def checkPwned(checkPassword: CheckPwned): Task[CheckedPwned] = ZIO.scoped {
    for
      occurrences <- repository.checkPwned(checkPassword.passwordHash).span(tracing, "check pwned query")
      dto = CheckedPwned(
        checkPassword.passwordHash,
        occurrences > 0,
        if occurrences > 0 then Some(occurrences) else None
      )
      _ <- ZIO.unit.tagMultiple(tracing, Map("passwordHash" -> dto.passwordHash, "pwnd" -> (occurrences > 0).toString))
      _ <- kafka.produce(writeToString(Event.CheckedPwned(dto.passwordHash)))
    yield dto
  }

  override def hashPassword(hashPassword: HashPassword): Task[HashedPassword] = ZIO.scoped {
    for
      normalizedPassword <- ZIO.succeed(normalizeHashPassword(hashPassword))
      dto                <- hash(normalizedPassword)
      _                  <- ZIO.unit.tagMultiple(tracing, Map("hashType" -> dto.hashType, "password" -> dto.password))
      _                  <- kafka.produce(writeToString(Event.HashedPassword(dto.password, dto.hashType)))
      _                  <- repository.saveHash(dto).span(tracing, "save hash query")
    yield dto
  }

  private def chooseHashAlgorithm(hashType: String): Task[String => String] = hashType match
    case "sha256" => ZIO.succeed(DigestUtils.sha256Hex)
    case _ =>
      ZIO.logError(s"Unsupported hash type: $hashType") *> ZIO.fail(RuntimeException("Unsupported hash type"))

  private def normalizeHashPassword(hashPassword: HashPassword): HashPassword =
    hashPassword.copy(hashType = hashPassword.hashType.toLowerCase())

  private def hash(password: HashPassword) = for
    hashAlgorithm <- chooseHashAlgorithm(password.hashType)
    hash = hashAlgorithm(password.password)
  yield HashedPassword(password.hashType, password.password, hash)

object PasswordService:
  val layer = ZLayer.fromFunction(PasswordServiceImpl.apply)
