package com.calvin.kafkaexample

import cats.effect.Sync
import cats.implicits._

final case class KafkaConfig(server: String, groupId: String, enableAutoCommit: String, autoCommitIntervalMs: String)
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
