package com.virtuslab.vss.common

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.upickle.*
import upickle.default.*

object BaseEndpoints:

  val checkPasswordEndpoint: Endpoint[Unit, CheckPassword, Unit, CheckedPassword, Any] = sttp.tapir.endpoint.post
    .in("check")
    .in(jsonBody[CheckPassword].example(CheckPassword("password")))
    .out(jsonBody[CheckedPassword].example(CheckedPassword(true)))

  val hashPasswordEndpoint: Endpoint[Unit, HashPassword, Unit, HashedPassword, Any] = sttp.tapir.endpoint.post
    .in("hash")
    .in(jsonBody[HashPassword].example(HashPassword("SHA256", "password")))
    .out(
      jsonBody[HashedPassword]
        .example(HashedPassword("SHA256", "password", "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"))
    )

case class CheckPassword(password: String)
object CheckPassword:
  given ReadWriter[CheckPassword] = macroRW

case class CheckedPassword(pwned: Boolean)
object CheckedPassword:
  given ReadWriter[CheckedPassword] = macroRW

case class HashPassword(hashType: String, password: String)
object HashPassword:
  given ReadWriter[HashPassword] = macroRW

case class HashedPassword(hashType: String, password: String, hash: String)
object HashedPassword:
  given ReadWriter[HashedPassword] = macroRW