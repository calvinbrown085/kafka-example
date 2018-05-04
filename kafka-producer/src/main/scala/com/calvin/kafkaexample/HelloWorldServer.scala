package com.calvin.kafkaexample

import cats.effect._
import cats.implicits._
import cats.effect.{Effect, IO}
import fs2.{Scheduler, Stream, StreamApp}
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
      k = new Kafka(conf.kafkaConfig)
      stream <- BlazeBuilder[IO]
          .bindHttp(8080, "0.0.0.0")
          .mountService(helloWorldService, "/")
          .serve.concurrently(k.queueThings(scheduler))
    } yield stream


}