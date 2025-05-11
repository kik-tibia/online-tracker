package com.kiktibia.onlinetracker.altfinder.bot

import com.kiktibia.onlinetracker.altfinder.bot.command.Command
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.syntax.all.*

class BotListener[F[_]: Async](commands: List[Command[F]], dispatcher: Dispatcher[F]) extends ListenerAdapter {

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    event.deferReply().queue()
    commands.find(_.command.getName == event.getName) match {
      case Some(command) => dispatcher.unsafeRunAndForget {
          command.handleEvent(event).flatMap { embed => Async[F].delay(event.getHook.sendMessageEmbeds(embed).queue()) }
        }
      case None => println(s"Command not found: ${event.getName}")
    }
  }

}
