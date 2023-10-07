package com.kiktibia.onlinetracker.altfinder

import cats.implicits.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.altfinder.bot.BotListener
import com.kiktibia.onlinetracker.altfinder.bot.command.FindAltsCommand
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import com.kiktibia.onlinetracker.common.config.BotConfig
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction

import scala.jdk.CollectionConverters.*
import cats.effect.IO

class DiscordBot(cfg: BotConfig, service: AltFinderService[IO]) {

  private val findAltsCommand = new FindAltsCommand(service)
  private val commands = List(findAltsCommand)
  private val botListener = new BotListener(commands)

  private val jda = JDABuilder.createDefault(cfg.token).addEventListeners(botListener).build()
  jda.awaitReady()
  println("JDA ready")

  List(jda.updateCommands()).map(updateCommands)

  private def updateCommands(c: CommandListUpdateAction): Unit = {
    c.addCommands(commands.map(_.command).asJava).complete()
  }

}
