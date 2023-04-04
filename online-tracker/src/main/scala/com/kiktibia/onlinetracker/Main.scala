package com.kiktibia.onlinetracker

import cats.effect.*
import cats.syntax.all.*
import ciris.*
import com.kiktibia.onlinetracker.repo.OnlineTrackerRepoImpl
import com.kiktibia.onlinetracker.service.OnlineTrackerService
import com.kiktibia.onlinetracker.tibiadata.{TibiaDataClient, TibiaDataClientImpl}
import fs2.Stream
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk.Session

import scala.concurrent.duration.*

object Main extends IOApp {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  final case class DatabaseConfig(
    host: String, port: Int, user: String, database: String, password: String
  )

  private val databaseConfig: ConfigValue[Effect, DatabaseConfig] =
    (
      env("DB_HOST").as[String],
      env("DB_PORT").as[Int],
      env("DB_USER").as[String],
      env("DB_NAME").as[String],
      env("DB_PASSWORD").as[String]
    ).parMapN(DatabaseConfig.apply)

  def run(args: List[String]): IO[ExitCode] = {
    databaseConfig.load[IO].flatMap { cfg =>
      val session: Resource[IO, Session[IO]] =
        Session.single(
          host = cfg.host,
          port = cfg.port,
          user = cfg.user,
          database = cfg.database,
          password = cfg.password.some)

      session.use { s =>
        val repo = new OnlineTrackerRepoImpl(s)
        TibiaDataClient.clientResource.use { client =>
          val tibiaDataClient = new TibiaDataClientImpl(client)
          val service = new OnlineTrackerService(repo, tibiaDataClient)

          Stream.fixedRateStartImmediately[IO](60.seconds).evalTap { _ =>
            service.updateDataForWorld("Nefera")
              .handleErrorWith { e =>
                Logger[IO].warn(e)("Recovering from error in stream:\n")
              }
          }.compile.drain.map(_ => ExitCode.Success)
        }
      }
    }
  }

}
