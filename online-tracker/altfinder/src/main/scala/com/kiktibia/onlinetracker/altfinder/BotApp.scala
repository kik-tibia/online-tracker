package com.kiktibia.onlinetracker.altfinder

import cats.effect.*
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraper
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraperHttp4sClient
import com.kiktibia.onlinetracker.altfinder.bot.BotListener
import com.kiktibia.onlinetracker.altfinder.bot.command.FindAltsCommand
import com.kiktibia.onlinetracker.altfinder.repo.AltFinderSkunkRepo
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import com.kiktibia.onlinetracker.common.config.AppConfig
import com.kiktibia.onlinetracker.common.repo.SessionResource
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object BotApp extends IOApp {

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    Dispatcher.parallel[IO].use { dispatcher =>
      AppConfig.config.load[IO].flatMap { cfg =>
        val dbSessionResource = SessionResource.single(cfg.database)
        val httpClientResource: Resource[IO, Client[IO]] = BazaarScraperHttp4sClient.clientResource
        val jdaResource: Resource[IO, JDA] = Resource.eval(IO.delay(JDABuilder.createDefault(cfg.bot.token).build()))

        (dbSessionResource, httpClientResource, jdaResource).tupled.use { case (dbSession, httpClient, jda) =>
          val bazaarScraperClient = new BazaarScraperHttp4sClient(httpClient)
          val bazaarScraper = new BazaarScraper(bazaarScraperClient)
          val repo = new AltFinderSkunkRepo(dbSession)
          val service = new AltFinderService(repo, bazaarScraper)
          val findAltsCommand = new FindAltsCommand[IO](service)
          val commands = List(findAltsCommand)
          val botListener = new BotListener[IO](commands, dispatcher)
          jda.addEventListener(botListener)
          DiscordBot.resource(cfg.bot, commands, dispatcher)

          IO.never
        }
      }
    }
  }

}
