package com.calvin.kafkaexample

import cats.effect._
import cats.implicits._
import cats.effect.{Effect, IO}
import com.calvin.kafkaexample.twitter.TwitterStream
import fs2.{Scheduler, Stream, StreamApp}
import org.http4s.client.blaze.Http1Client
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object HelloWorldServer extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) = ServerStream.stream[IO]
}

object ServerStream {

  def helloWorldService[F[_]: Effect] = new HelloWorldService[F].service

  def stream[F[_]: Effect](implicit ec: ExecutionContext): Stream[IO, StreamApp.ExitCode] =
    for {
      conf <- Stream.eval(Config.loadServiceConfig[IO])
      scheduler <- Scheduler[IO](5)
      client <- Stream.eval(Http1Client[IO]())
      k = new Kafka(conf.kafkaConfig)
      _ = k.createTopic()
      stream <- BlazeBuilder[IO]
          .bindHttp(8081, "0.0.0.0")
          .mountService(helloWorldService, "/")
          .serve concurrently Stream.eval(TwitterStream.twitterStream(client, k))
    } yield stream
}
