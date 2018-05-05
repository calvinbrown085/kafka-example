package com.calvin.kafkaexample

import cats.effect.IO
import java.util
import java.util.Properties
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import fs2.Scheduler


object Main {
  def main(args: Array[String]): Unit = {
    val myTopic = "my-topic-2"
    import org.apache.kafka.clients.consumer.KafkaConsumer

    val props = new Properties()
    props.put("bootstrap.servers", "192.168.99.100:32400")
    props.put("group.id", "test")
    props.put("enable.auto.commit", "true")
    props.put("auto.commit.interval.ms", "1000")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    val consumer = new KafkaConsumer[String, String](props)


    consumer.subscribe(util.Arrays.asList(myTopic))

    while (true) {
      val records = consumer.poll(100)
      import scala.collection.JavaConversions._
      records.records(myTopic).map { record =>
        println(s"offset = ${record.offset}, key = ${record.key}, value = ${record.value}")
      }
    }
  }
}
