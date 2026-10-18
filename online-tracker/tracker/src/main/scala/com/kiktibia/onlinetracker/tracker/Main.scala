package com.kiktibia.onlinetracker.tracker

import cats.effect.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.common.config.AppConfig
import com.kiktibia.onlinetracker.common.repo.SessionResource
import com.kiktibia.onlinetracker.tracker.repo.OnlineTrackerSkunkRepo
import com.kiktibia.onlinetracker.tracker.service.OnlineTrackerService
import com.kiktibia.onlinetracker.tracker.tibiadata.TibiaDataHttp4sClient
import fs2.Stream
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

object Main extends IOApp {

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    AppConfig.config.load[IO].flatMap { cfg =>
      val dbSessionResource = SessionResource.single(cfg.database)
      val tibiaDataClientResource = TibiaDataHttp4sClient.clientResource

      (dbSessionResource, tibiaDataClientResource).tupled.use { case (dbSession, tibiaDataClientSession) =>
        val repo = new OnlineTrackerSkunkRepo(dbSession)
        val tibiaDataClient = new TibiaDataHttp4sClient(tibiaDataClientSession)
        val service = new OnlineTrackerService(repo, tibiaDataClient)

        Stream.fixedRateStartImmediately[IO](15.seconds).evalTap { _ =>
          service.updateDataForWorld("Nefera").handleErrorWith { e =>
            Logger[IO].warn(e)(s"Recovering from error in stream:${System.lineSeparator}")
          }
        }.compile.drain.as(ExitCode.Success)
      }
    }
  }

}
