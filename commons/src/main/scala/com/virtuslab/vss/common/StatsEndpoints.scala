package com.virtuslab.vss.common

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.upickle.*
import upickle.default.*

object StatsEndpoints:

  val getAllEvents: Endpoint[Unit, Unit, Unit, List[Event], Any] = sttp.tapir.endpoint.get
    .in("allevents")
    .out(jsonBody[List[Event]].example(List(Event.CheckedPwned("5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"))))