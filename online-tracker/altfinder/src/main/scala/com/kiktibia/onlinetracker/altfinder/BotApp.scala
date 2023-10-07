package com.kiktibia.onlinetracker.altfinder

import cats.effect.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.altfinder.repo.AltFinderSkunkRepo
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import com.kiktibia.onlinetracker.common.config.AppConfig
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.otel4s.trace.Tracer
import skunk.Session

object BotApp extends IOApp {

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
        val program = {
          val repo = new AltFinderSkunkRepo(s)
          val service = new AltFinderService(repo)
          val bot = new DiscordBot(cfg.bot, service)
          IO.unit
        }
        program.toResource.useForever.map(_ => ExitCode.Success)
      }
    }

  }

}
