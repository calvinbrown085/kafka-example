package com.calvin.kafkaexample

import java.util.{Properties, UUID}

import cats.effect.IO
import com.calvin.kafkaexample.twitter.BasicTweet
import fs2.{Scheduler, Stream}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, NewTopic}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.collection.JavaConverters._
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
  val myTopic = "twitter-stream-1"
  val partitionCount = 3
  var c = 0

  def queueTweets(basicTweet: BasicTweet) = Stream.eval{
    producer.send(new ProducerRecord[String, String](myTopic, UUID.randomUUID().toString, basicTweet.toString))
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
