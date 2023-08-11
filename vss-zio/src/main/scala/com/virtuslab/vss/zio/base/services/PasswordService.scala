package com.virtuslab.vss.zio.base.services

import com.virtuslab.vss.common.{CheckPwned, CheckedPwned, HashPassword, HashedPassword}
import zio.*
import upickle.default.*
import org.apache.commons.codec.digest.DigestUtils
import com.virtuslab.vss.common.Event
import zio.telemetry.opentracing.OpenTracing
import com.virtuslab.vss.zio.base.resources.TracingOps.*
import com.virtuslab.vss.zio.base.resources.*
import com.virtuslab.vss.zio.base.repositories.PasswordRepository
import com.virtuslab.vss.zio.base.resources.TracingOps

trait PasswordService:
  def checkPwned(checkPassword: CheckPwned): Task[CheckedPwned]
  def hashPassword(hashPassword: HashPassword): Task[HashedPassword]

case class PasswordServiceImpl(repository: PasswordRepository, kafka: KafkaProducer, tracing: OpenTracing)
  extends PasswordService:

  override def checkPwned(checkPassword: CheckPwned): Task[CheckedPwned] = ZIO.scoped {
    for
      pwnd <- repository.checkPwned(checkPassword.passwordHash).span(tracing, "check pwned query")
      dto = CheckedPwned(checkPassword.passwordHash, pwnd)
      _ <- ZIO.unit.tagMultiple(tracing, Map("passwordHash" -> dto.passwordHash, "pwnd" -> pwnd.toString))
      _ <- kafka.produce(write(Event.CheckedPwned(dto.passwordHash)))
    yield dto
  }

  override def hashPassword(hashPassword: HashPassword): Task[HashedPassword] = ZIO.scoped {
    for
      normalizedPassword <- ZIO.succeed(normalizeHashPassword(hashPassword))
      foundPassword <- repository.getHash(hashPassword.hashType, hashPassword.password).span(tracing, "get hash query")
      dto <- foundPassword match
        case Some(value) =>
          ZIO
            .logInfo(
              s"Using cached hash for hashType = ${hashPassword.hashType} and password = ${hashPassword.password}}"
            )
            .map(_ => value)
        case None => hash(normalizedPassword)
      _ <- ZIO.unit.tagMultiple(tracing, Map("hashType" -> dto.hashType, "password" -> dto.password))
      _ <- kafka.produce(write(Event.HashedPassword(dto.password, dto.hashType)))
      _ <- repository.saveHash(dto).span(tracing, "save hash query")
    yield dto
  }

  private def chooseHashAlgorithm(hashType: String): Task[String => String] = hashType match
    case "sha256" => ZIO.succeed(DigestUtils.sha256Hex)
    case _ =>
      ZIO
        .logError(s"Unsupported hash type: $hashType")
        .flatMap(_ => ZIO.fail(new RuntimeException("Unsupported hash type")))

  private def normalizeHashPassword(hashPassword: HashPassword): HashPassword =
    hashPassword.copy(hashType = hashPassword.hashType.toLowerCase())

  private def hash(password: HashPassword) = for
    hashAlgorithm <- chooseHashAlgorithm(password.hashType)
    hash = hashAlgorithm(password.password)
  yield HashedPassword(password.hashType, password.password, hash)

object PasswordService:
  val layer = ZLayer.fromFunction(PasswordServiceImpl.apply)
