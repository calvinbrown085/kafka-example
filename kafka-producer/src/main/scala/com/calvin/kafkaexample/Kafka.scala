package com.calvin.kafkaexample

import java.util.{Properties, UUID}

import cats.effect.{Effect, IO}
import fs2.{Scheduler, Stream}
import scala.collection.JavaConverters._
import org.apache.kafka.clients.admin._
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization._
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
  val myTopic = "my-topic-3"
  val partitionCount = 2

  def queueThings(scheduler: Scheduler): Stream[IO, Unit] = {
    scheduler.awakeEvery[IO](1.second).evalMap(_ => IO(producer.send(new ProducerRecord[String, String](myTopic, UUID.randomUUID().toString, "This is a message"))))
  }

  def createTopic() = {
    val adminClientConfig = Map[String, Object](
      AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG -> kafkaConfig.server
    )
    val adminClient = AdminClient.create(adminClientConfig.asJava)
    if (!adminClient.listTopics().names().get().asScala.contains(myTopic)) {
      adminClient.createTopics(List(new NewTopic(myTopic, partitionCount, 2)).asJava)
    }
    adminClient.close()

  }
}
