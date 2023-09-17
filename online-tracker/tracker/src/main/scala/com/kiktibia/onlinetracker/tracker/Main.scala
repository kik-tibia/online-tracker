package com.kiktibia.onlinetracker.tracker

import cats.effect.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.common.config.{AppConfig, DatabaseConfig}
import com.kiktibia.onlinetracker.tracker.repo.OnlineTrackerSkunkRepo
import com.kiktibia.onlinetracker.tracker.service.OnlineTrackerService
import com.kiktibia.onlinetracker.tracker.tibiadata.TibiaDataHttp4sClient
import fs2.Stream
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk.Session

import scala.concurrent.duration.*

object Main extends IOApp {

  given Logger[IO] = Slf4jLogger.getLogger[IO]
  given Tracer[IO] = Tracer.noop

  override def run(args: List[String]): IO[ExitCode] = {
    AppConfig.config.load[IO].flatMap { cfg =>
      val dbCfg = cfg.database
      val session: Resource[IO, Session[IO]] = Session.single(
        host = dbCfg.host,
        port = dbCfg.port,
        user = dbCfg.user,
        database = dbCfg.database,
        password = dbCfg.password.some
      )

      session.use { s =>
        val repo = new OnlineTrackerSkunkRepo(s)
        TibiaDataHttp4sClient.clientResource.use { client =>
          val tibiaDataClient = new TibiaDataHttp4sClient(client)
          val service = new OnlineTrackerService(repo, tibiaDataClient)

          Stream.fixedRateStartImmediately[IO](15.seconds).evalTap { _ =>
            service.updateDataForWorld("Nefera").handleErrorWith { e =>
              Logger[IO].warn(e)(s"Recovering from error in stream:${System.lineSeparator}")
            }
          }.compile.drain.map(_ => ExitCode.Success)
        }
      }
    }
  }

}
