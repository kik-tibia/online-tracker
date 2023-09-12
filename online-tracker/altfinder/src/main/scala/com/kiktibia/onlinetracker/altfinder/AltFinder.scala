package com.kiktibia.onlinetracker.altfinder

import cats.effect.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.altfinder.repo.AltFinderSkunkRepo
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import com.kiktibia.onlinetracker.common.config.{AppConfig, DatabaseConfig}
import fs2.Stream
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk.Session

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneId, ZoneOffset}
import scala.concurrent.duration.*

object AltFinder extends IOApp {

  given Logger[IO] = Slf4jLogger.getLogger[IO]
  given Tracer[IO] = Tracer.noop

  override def run(args: List[String]): IO[ExitCode] = {
    AppConfig.databaseConfig.load[IO].flatMap { cfg =>
      val session: Resource[IO, Session[IO]] = Session.single(
        host = cfg.host,
        port = cfg.port,
        user = cfg.user,
        database = cfg.database,
        password = cfg.password.some
      )

      session.use { s =>
        val repo = new AltFinderSkunkRepo(s)
        val service = new AltFinderService(repo)

        val from = OffsetDateTime.parse("2023-09-03T10:00:00+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME).some
        // val to = OffsetDateTime.parse("2023-08-10T10:00:00+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME).some
        // val from = None
        // val from = OffsetDateTime.now().minusDays(20).some
        val to = None

        for
          _ <- Logger[IO].info("Running alt finder")
          _ <- service.findAndPrintAlts(List("kikaro"), from, to)
          _ <- Logger[IO].info("Done")
        yield ExitCode.Success
      }
    }
  }

}
