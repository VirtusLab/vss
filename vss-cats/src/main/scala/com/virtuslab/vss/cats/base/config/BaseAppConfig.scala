package com.virtuslab.vss.cats.base.config

case class BaseAppConfig(
  httpHost: String,
  httpPort: Int,
  grpcHost: String,
  grpcPort: Int,
  postgresHost: String,
  postgresPort: Int,
  dbName: String,
  postgresUser: String,
  postgresPassword: String,
  kafkaHost: String,
  kafkaPort: Int,
  jaegerUri: String
)
