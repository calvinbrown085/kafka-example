package com.calvin.kafkaexample

import java.util
import java.util.Properties


object Main {
  def main(args: Array[String]): Unit = {
    import org.apache.kafka.clients.consumer.KafkaConsumer

    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("group.id", "test")
    props.put("enable.auto.commit", "true")
    props.put("auto.commit.interval.ms", "1000")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    val consumer = new KafkaConsumer[String, String](props)


    consumer.subscribe(util.Arrays.asList("my-topic"))

    while (true) {
      val records = consumer.poll(100)
      import scala.collection.JavaConversions._
      records.records("my-topic").map { record =>
        println(s"offset = ${record.offset}, key = ${record.key}, value = ${record.value}")
      }
    }
  }
}
