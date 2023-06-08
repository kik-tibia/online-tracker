package com.kiktibia.onlinetracker.altfinder

import cats.effect.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.altfinder.repo.AltFinderSkunkRepo
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import com.kiktibia.onlinetracker.config.{AppConfig, DatabaseConfig}
import fs2.Stream
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk.Session

import java.time.OffsetDateTime
import scala.concurrent.duration.*

object AltFinder extends IOApp {

  given Logger[IO] = Slf4jLogger.getLogger[IO]

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
        val repo = new AltFinderSkunkRepo(s)
        val service = new AltFinderService(repo)

        val from = OffsetDateTime.now().minusDays(7).some
        val to = None
        for
          _ <- Logger[IO].info("Running alt finder")
          _ <- service.findAndPrintAlts(
            List(
              "kikaro", "goanna kendrick"
            ), from, to
          )
          _ <- Logger[IO].info("Done")
        yield ExitCode.Success
      }
    }
  }

}
