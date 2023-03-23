package com.virtuslab.vss.common

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.upickle.*
import upickle.default.*

object CheckPasswordHttpEndpoints:

  val checkPasswordEndpoint: Endpoint[Unit, CheckPassword, Unit, CheckedPassword, Any] = sttp.tapir.endpoint.post
    .in("check")
    .in(jsonBody[CheckPassword].example(CheckPassword("password")))
    .out(jsonBody[CheckedPassword].example(CheckedPassword(true)))

case class CheckPassword(password: String)
object CheckPassword:
  given ReadWriter[CheckPassword] = macroRW

case class CheckedPassword(pwned: Boolean)
object CheckedPassword:
  given ReadWriter[CheckedPassword] = macroRW
