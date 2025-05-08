package com.kiktibia.onlinetracker.altfinder

import cats.implicits.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.altfinder.bot.BotListener
import com.kiktibia.onlinetracker.altfinder.bot.command.FindAltsCommand
import com.kiktibia.onlinetracker.altfinder.bot.command.Command
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import com.kiktibia.onlinetracker.common.config.BotConfig
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction

import scala.jdk.CollectionConverters.*
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import net.dv8tion.jda.api.JDA

class DiscordBot[F[_]: Async](
    jda: JDA,
    commands: List[Command[F]],
    dispatcher: Dispatcher[F]
    // service: AltFinderService[IO]
) {

  // private val findAltsCommand = new FindAltsCommand(service)
  // private val commands = List(findAltsCommand)
  private val botListener = new BotListener[F](commands, dispatcher)

  // private val jda = JDABuilder.createDefault(cfg.token).addEventListeners(botListener).build()
  // jda.awaitReady()
  // println("JDA ready")

  // List(jda.updateCommands()).map(updateCommands)
  //
  // private def updateCommands(c: CommandListUpdateAction): Unit = {
  //   c.addCommands(commands.map(_.command).asJava).complete()
  // }

  def registerCommands(): F[Unit] = Async[F].delay {
    val update: CommandListUpdateAction = jda.updateCommands()
    update.addCommands(commands.map(_.command).asJava).complete()
  }.void

}

object DiscordBot {

  def resource[F[_]: Async](
      cfg: BotConfig,
      commands: List[Command[F]],
      dispatcher: Dispatcher[F]
  ): Resource[F, DiscordBot[F]] = {
    for {
      jda <- Resource.eval(Async[F].delay(JDABuilder.createDefault(cfg.token).build()))
      _ <- Resource.eval(Async[F].delay(jda.awaitReady()))
      bot = new DiscordBot[F](jda, commands, dispatcher)
      _ <- Resource.eval(bot.registerCommands())
    } yield bot

  }
}
