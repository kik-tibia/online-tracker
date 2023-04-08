package com.kiktibia.onlinetracker.config

import cats.syntax.all.*
import ciris.*

object AppConfig {
  val databaseConfig: ConfigValue[Effect, DatabaseConfig] = (
    env("DB_HOST").as[String],
    env("DB_PORT").as[Int],
    env("DB_USER").as[String],
    env("DB_NAME").as[String],
    env("DB_PASSWORD").as[String]
  ).parMapN(DatabaseConfig.apply)
}
final case class DatabaseConfig(
  host: String, port: Int, user: String, database: String, password: String
)
