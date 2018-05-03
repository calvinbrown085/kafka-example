package com.calvin.kafkaexample

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

object Kafka {

  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("acks", "all")
  props.put("retries", "0")
  props.put("batch.size", "16384")
  props.put("linger.ms", "1")
  props.put("buffer.memory", "33554432")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, String](props)

  def r = {
    val l = List.fill(100)(1)
    l.map(i => producer.send(new ProducerRecord[String, String]("my-topic", i.toString, i.toString)))
    producer.close()
  }
}
