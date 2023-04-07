package com.virtuslab.vss.cats.base.config

case class AppConfig(
  httpPort: Int,
  httpHost: String,
  grpcPort: Int,
  grpcHost: String,
  postgresPort: Int,
  postgresHost: String,
  dbName: String,
  postgresUser: String,
  postgresPassword: String,
  kafkaPort: Int,
  kafkaHost: String,
)
