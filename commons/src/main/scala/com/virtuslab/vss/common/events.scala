package com.virtuslab.vss.common

import upickle.default.*

enum Event:
  case HashedPassword(password: String, hashType: String)
  case CheckedPwned(email: String)

object Event {
  given ReadWriter[Event] = ReadWriter.merge(
    macroRW[Event.HashedPassword],
    macroRW[Event.CheckedPwned]
  )
}
