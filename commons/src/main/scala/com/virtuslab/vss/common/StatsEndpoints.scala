package com.virtuslab.vss.common

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.upickle.*
import upickle.default.*

object StatsEndpoints:

  val getAllEvents: Endpoint[Unit, Unit, Unit, List[Event], Any] = sttp.tapir.endpoint.get
    .in("allevents")
    .out(jsonBody[List[Event]].example(List(Event.CheckedPwned("michael.scott@dundermifflin.com"))))