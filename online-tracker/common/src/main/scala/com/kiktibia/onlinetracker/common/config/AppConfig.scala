package com.kiktibia.onlinetracker.common.config

import cats.syntax.all.*
import ciris.*

final case class DatabaseConfig(host: String, port: Int, user: String, database: String, password: String)

final case class BotConfig(token: String)

final case class Config(database: DatabaseConfig, bot: BotConfig)

object AppConfig {
  private val databaseConfig: ConfigValue[Effect, DatabaseConfig] = (
    env("DB_HOST").as[String],
    env("DB_PORT").as[Int],
    env("DB_USER").as[String],
    env("DB_NAME").as[String],
    env("DB_PASSWORD").as[String]
  ).parMapN(DatabaseConfig.apply)

  private val botConfig: ConfigValue[Effect, BotConfig] = (env("TOKEN").as[String].map(BotConfig.apply))

  val config: ConfigValue[Effect, Config] = (databaseConfig, botConfig).parMapN(Config.apply)
}
