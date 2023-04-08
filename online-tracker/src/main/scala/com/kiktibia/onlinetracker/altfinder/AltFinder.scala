package com.kiktibia.onlinetracker.altfinder

import cats.effect.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.altfinder.repo.AltFinderRepoImpl
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import com.kiktibia.onlinetracker.config.{AppConfig, DatabaseConfig}
import fs2.Stream
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import skunk.Session

import scala.concurrent.duration.*

object AltFinder extends IOApp {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

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
        val repo = new AltFinderRepoImpl(s)
        val service = new AltFinderService(repo)
        for
          _ <- Logger[IO].info("Running alt finder")
          _ <- service.printOnlineTimes(List(
            "kikaro"
          ))
        yield ExitCode.Success
      }
    }
  }

}
