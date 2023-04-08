package com.kiktibia.onlinetracker.tracker

import cats.effect.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.config.{AppConfig, DatabaseConfig}
import com.kiktibia.onlinetracker.tracker.repo.OnlineTrackerSkunkRepo
import com.kiktibia.onlinetracker.tracker.service.OnlineTrackerService
import com.kiktibia.onlinetracker.tracker.tibiadata.TibiaDataHttp4sClient
import fs2.Stream
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk.Session

import scala.concurrent.duration.*

object Main extends IOApp {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  override def run(args: List[String]): IO[ExitCode] = {
    AppConfig.databaseConfig.load[IO].flatMap { cfg =>
      val session: Resource[IO, Session[IO]] =
        Session.single(
          host = cfg.host,
          port = cfg.port,
          user = cfg.user,
          database = cfg.database,
          password = cfg.password.some)

      session.use { s =>
        val repo = new OnlineTrackerSkunkRepo(s)
        TibiaDataHttp4sClient.clientResource.use { client =>
          val tibiaDataClient = new TibiaDataHttp4sClient(client)
          val service = new OnlineTrackerService(repo, tibiaDataClient)

          Stream.fixedRateStartImmediately[IO](60.seconds).evalTap { _ =>
            service.updateDataForWorld("Nefera")
              .handleErrorWith { e =>
                Logger[IO].warn(e)(s"Recovering from error in stream:${System.lineSeparator}")
              }
          }.compile.drain.map(_ => ExitCode.Success)
        }
      }
    }
  }

}
