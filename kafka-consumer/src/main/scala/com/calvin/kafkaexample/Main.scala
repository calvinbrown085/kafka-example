package com.calvin.kafkaexample

import cats.effect.IO
import java.util
import java.util.Properties

import scala.concurrent.ExecutionContext.Implicits.global
import fs2.Scheduler
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common._
import org.apache.kafka.common.serialization._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import cats.effect._
import cats.implicits._
import cats.effect.{Effect, IO}
import fs2.{Scheduler, Stream, StreamApp}
import org.http4s.HttpService
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder

object HelloWorldServer extends StreamApp[IO] with Http4sDsl[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) = ServerStream.stream[IO]

  val pingRoute = HttpService[IO] { case GET -> Root / "ping" => Ok("pong")}
}

object ServerStream extends Http4sDsl[IO]{


  def stream[F[_]: Effect]: Stream[IO, StreamApp.ExitCode] =
    for {
      conf <- Stream.eval(Config.loadServiceConfig[IO])
      scheduler <- Scheduler[IO](5)
      kafkaConsumer = KafkaConsumer.createKafkaConsumer[String, String]
      stream <- BlazeBuilder[IO]
          .bindHttp(8080, "0.0.0.0")
          .mountService(HelloWorldServer.pingRoute, "/")
          .serve concurrently KafkaConsumer.consume[IO, String, String](scheduler, kafkaConsumer)
    } yield stream
}
object KafkaConsumer {
  val myTopic = "my-topic-7"


  def consume[F[_], A, B](scheduler: Scheduler, consumer: KafkaConsumer[A, B])(implicit F: Effect[F]): Stream[F, Iterable[Unit]] = {
    consumer.subscribe(util.Arrays.asList(myTopic))
    scheduler.awakeEvery(0.1.seconds).evalMap(_ => F.delay{
      val records = consumer.poll(100)
      import scala.collection.JavaConversions._
      records.records(myTopic).map { record =>
        println(s"offset = ${record.offset}, key = ${record.key}, value = ${record.value}")
      }
    })
  }

  def createKafkaConsumer[A, B]: KafkaConsumer[A, B] = {
    val consumerConfig = Map[String, Object](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> "192.168.99.100:32400",
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> "true",
      ConsumerConfig.GROUP_ID_CONFIG -> "test",
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer].getName,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer].getName
    )
    new KafkaConsumer[A, B](consumerConfig.asJava)
  }
}
