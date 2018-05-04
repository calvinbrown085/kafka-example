package com.calvin.kafkaexample

import java.util.{Properties, UUID}

import cats.effect.{Effect, IO}
import fs2.{Scheduler, Stream}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

final case class Kafka(kafkaConfig: KafkaConfig) {

  val props = new Properties()
  props.put("bootstrap.servers", kafkaConfig.server)
  props.put("acks", kafkaConfig.acks)
  props.put("retries", kafkaConfig.retries)
  props.put("batch.size", kafkaConfig.batchSize)
  props.put("linger.ms", kafkaConfig.lingerMs)
  props.put("buffer.memory", kafkaConfig.bufferMemory)
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, String](props)

  def queueThings(scheduler: Scheduler): Stream[IO, Unit] = {
    scheduler.awakeEvery[IO](1.second).evalMap(_ => IO(producer.send(new ProducerRecord[String, String]("my-topic", UUID.randomUUID().toString, "This is a message"))))
  }
}
