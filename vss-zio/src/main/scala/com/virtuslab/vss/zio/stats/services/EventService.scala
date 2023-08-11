package com.virtuslab.vss.zio.stats.services

import zio.*
import com.virtuslab.vss.common.Event

trait EventService:
  def listEvents(): Task[List[Event]]
  def saveEvent(event: Event): Task[Unit]

case class EventServiceImpl(events: Ref[List[Event]]) extends EventService:

  override def listEvents(): Task[List[Event]] = events.get

  override def saveEvent(event: Event): Task[Unit] = events.update(_ :+ event)

object EventService:
  val layer = ZLayer.fromZIO(
    for events <- Ref.make(List[Event]())
    yield EventServiceImpl(events)
  )
