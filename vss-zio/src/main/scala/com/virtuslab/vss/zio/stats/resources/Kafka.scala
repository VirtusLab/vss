package com.virtuslab.vss.zio.stats.resources

import zio.*
import zio.stream.*
import zio.kafka.serde.Serde
import com.virtuslab.vss.zio.stats.config.KafkaConfig
import com.virtuslab.vss.zio.stats.services.EventService
import zio.kafka.consumer.{CommittableRecord, Consumer, ConsumerSettings, Subscription}
import com.virtuslab.vss.common.Event
import upickle.default.*

trait KafkaConsumer:
  def consume: Stream[Throwable, Nothing]

case class KafkaConsumerImpl(consumer: Consumer, eventService: EventService) extends KafkaConsumer:
  private val topicName = "events"

  override def consume: Stream[Throwable, Nothing] =
    consumer
      .plainStream(Subscription.topics(topicName), Serde.string, Serde.string)
      .mapZIO { case committableRecord =>
        for
          event <- ZIO.attempt(read[Event](committableRecord.record.value()))
          _ <- eventService.saveEvent(event)
        yield committableRecord
      }
      .map(_.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapZIO(_.commit)
      .drain

object KafkaConsumer:
  private val groupId = "vss-consumer-group"

  private val consumer: TaskLayer[Consumer] = ZLayer.scoped {
    for
      config <- ZIO.config(KafkaConfig.config)
      servers = List(s"${config.host}:${config.port}")
      consumer <- Consumer.make(ConsumerSettings(servers).withGroupId(groupId))
    yield consumer
  }

  val layer: RLayer[EventService, KafkaConsumer] = consumer >>> ZLayer.fromFunction(KafkaConsumerImpl.apply)
