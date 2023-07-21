package com.virtuslab.vss.cats.stats.config

case class StatsAppConfig(
  httpHost: String,
  httpPort: Int,
  grpcHost: String,
  grpcPort: Int,
  kafkaHost: String,
  kafkaPort: Int
)
