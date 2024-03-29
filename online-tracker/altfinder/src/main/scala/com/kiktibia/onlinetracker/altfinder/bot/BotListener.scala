package com.kiktibia.onlinetracker.altfinder.bot

import com.kiktibia.onlinetracker.altfinder.bot.command.Command
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class BotListener(commands: List[Command]) extends ListenerAdapter {

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    commands.find(_.command.getName == event.getName) match {
      case Some(command) =>
        event.deferReply().queue()
        val embed = command.handleEvent(event)
        event.getHook().sendMessageEmbeds(embed).queue()
      case None => println(s"Command not found: ${event.getName}")
    }
  }

}
