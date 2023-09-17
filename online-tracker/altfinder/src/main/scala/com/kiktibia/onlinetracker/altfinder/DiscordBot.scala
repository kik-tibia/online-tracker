package com.kiktibia.onlinetracker.altfinder

import cats.Applicative
import cats.Monad
import cats.effect.Sync
import cats.implicits.*
import cats.syntax.all.*
import io.circe.Error
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.lang.model.element.TypeElement
import scala.jdk.CollectionConverters.*
import com.kiktibia.onlinetracker.common.config.BotConfig
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import com.kiktibia.onlinetracker.altfinder.bot.command.FindAltsCommand
import com.kiktibia.onlinetracker.altfinder.bot.BotListener
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import cats.effect.IO

class DiscordBot(cfg: BotConfig, service: AltFinderService[IO]) {

  private val findAltsCommand = new FindAltsCommand(service)
  private val commands = List(findAltsCommand)
  private val botListener = new BotListener(commands)

  private val jda = JDABuilder.createDefault(cfg.token).addEventListeners(botListener).build()
  jda.awaitReady()
  println("JDA ready")

  private val testGuild = jda.getGuildById("")
  List(testGuild.updateCommands(), jda.updateCommands()).map(updateCommands)

  private def updateCommands(c: CommandListUpdateAction): Unit = {
    c.addCommands(commands.map(_.command).asJava).complete()
  }

}
