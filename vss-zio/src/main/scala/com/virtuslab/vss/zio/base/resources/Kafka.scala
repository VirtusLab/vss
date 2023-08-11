package com.virtuslab.vss.zio.base.resources

import zio.*
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import org.apache.kafka.clients.producer.RecordMetadata
import com.virtuslab.vss.zio.base.config.KafkaConfig

trait KafkaProducer:
  def produce(value: String): Task[RecordMetadata]

case class KafkaProducerImpl(producer: Producer) extends KafkaProducer:
  private val topicName = "events"
  private val key = "event"

  override def produce(value: String): Task[RecordMetadata] =
    producer.produce[Any, String, String](
      topic = topicName,
      key = key,
      value = value,
      keySerializer = Serde.string,
      valueSerializer = Serde.string
    )

object KafkaProducer:
  private val producerLayer: TaskLayer[Producer] = ZLayer.scoped {
    for
      config <- ZIO.config(KafkaConfig.config)
      producer <- Producer.make(
        ProducerSettings(List(s"${config.host}:${config.port}"))
      )
    yield producer
  }

  val layer: TaskLayer[KafkaProducer] = producerLayer >>> ZLayer.fromFunction(KafkaProducerImpl.apply)
