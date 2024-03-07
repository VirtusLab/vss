package com.virtuslab.vss.common

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import com.github.plokhotnyuk.jsoniter_scala.core.*

object BaseEndpoints:

  val checkPasswordEndpoint: PublicEndpoint[CheckPwned, Unit, CheckedPwned, Any] = endpoint.post
    .in("check")
    .in(jsonBody[CheckPwned].example(CheckPwned("5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8")))
    .out(
      jsonBody[CheckedPwned]
        .example(CheckedPwned("5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", true, Some(1)))
    )

  val hashPasswordEndpoint: PublicEndpoint[HashPassword, Unit, HashedPassword, Any] = endpoint.post
    .in("hash")
    .in(jsonBody[HashPassword].example(HashPassword("SHA256", "password")))
    .out(
      jsonBody[HashedPassword]
        .example(
          HashedPassword("SHA256", "password", "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8")
        )
    )

case class CheckPwned(passwordHash: String)
object CheckPwned:
  given JsonValueCodec[CheckPwned] = JsonCodecMaker.make

case class CheckedPwned(passwordHash: String, pwned: Boolean, occurrences: Option[Long])
object CheckedPwned:
  given JsonValueCodec[CheckedPwned] = JsonCodecMaker.make

case class HashPassword(hashType: String, password: String)
object HashPassword:
  given JsonValueCodec[HashPassword] = JsonCodecMaker.make

case class HashedPassword(hashType: String, password: String, hash: String)
object HashedPassword:
  given JsonValueCodec[HashedPassword] = JsonCodecMaker.make
