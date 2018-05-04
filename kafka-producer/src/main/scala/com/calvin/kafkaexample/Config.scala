package com.calvin.kafkaexample

import cats.effect.Sync
import cats.implicits._

final case class KafkaConfig(server: String, acks: String, retries: String, batchSize: String, lingerMs: String, bufferMemory: String)
final case class ServiceConfig(kafkaConfig: KafkaConfig)

object Config {
  def loadServiceConfig[F[_]](implicit F: Sync[F]): F[ServiceConfig] = {
    import com.typesafe.config.ConfigFactory
    import pureconfig.loadConfigOrThrow

    for {
      config <- F.delay(ConfigFactory.load())
      kafka <- F.delay(loadConfigOrThrow[KafkaConfig](config, "kafka"))
    } yield ServiceConfig(kafka)
  }
}
