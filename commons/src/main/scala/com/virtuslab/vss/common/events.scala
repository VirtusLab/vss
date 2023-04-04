package com.virtuslab.vss.common

import upickle.default.*

enum Event:
  case HashedPassword(password: String, hashType: String)
  case CheckedPassword(password: String)

object Event {
  given ReadWriter[Event] = ReadWriter.merge(
    macroRW[Event.HashedPassword],
    macroRW[Event.CheckedPassword]
  )
}
