package com.virtuslab.vss.common

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.upickle.*
import upickle.default.*

object BaseEndpoints:

  val checkPasswordEndpoint: Endpoint[Unit, CheckPwned, Unit, CheckedPwned, Any] = sttp.tapir.endpoint.post
    .in("check")
    .in(jsonBody[CheckPwned].example(CheckPwned("5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8")))
    .out(jsonBody[CheckedPwned].example(CheckedPwned("5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", true)))

  val hashPasswordEndpoint: Endpoint[Unit, HashPassword, Unit, HashedPassword, Any] = sttp.tapir.endpoint.post
    .in("hash")
    .in(jsonBody[HashPassword].example(HashPassword("SHA256", "password")))
    .out(
      jsonBody[HashedPassword]
        .example(HashedPassword("SHA256", "password", "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"))
    )

case class CheckPwned(passwordHash: String)
object CheckPwned:
  given ReadWriter[CheckPwned] = macroRW

case class CheckedPwned(passwordHash: String, pwned: Boolean)
object CheckedPwned:
  given ReadWriter[CheckedPwned] = macroRW

case class HashPassword(hashType: String, password: String)
object HashPassword:
  given ReadWriter[HashPassword] = macroRW

case class HashedPassword(hashType: String, password: String, hash: String)
object HashedPassword:
  given ReadWriter[HashedPassword] = macroRW