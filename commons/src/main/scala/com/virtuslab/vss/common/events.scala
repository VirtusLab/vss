package com.virtuslab.vss.common

import com.github.plokhotnyuk.jsoniter_scala.macros.*
import com.github.plokhotnyuk.jsoniter_scala.core.*

enum Event:
  case HashedPassword(password: String, hashType: String)
  case CheckedPwned(passwordHash: String)

object Event:
  given JsonValueCodec[List[Event]] = JsonCodecMaker.make
  given JsonValueCodec[Event]       = JsonCodecMaker.make
