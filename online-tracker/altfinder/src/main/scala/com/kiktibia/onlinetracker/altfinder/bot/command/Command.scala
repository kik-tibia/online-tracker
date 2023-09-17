package com.kiktibia.onlinetracker.altfinder.bot.command

import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

import java.time.format.TextStyle
import java.util.Locale
import cats.effect.unsafe.IORuntime
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.LocalTime

trait Command {

  given IORuntime = IORuntime.global

  val command: SlashCommandData

  val embedColour = 16763922

  def handleEvent(event: SlashCommandInteractionEvent): MessageEmbed

  def optionMappingToSSDateTime(option: OptionMapping): OffsetDateTime = {
    val date = LocalDate.parse(option.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE)
    ZonedDateTime.of(date, LocalTime.of(10, 0), ZoneId.of("Europe/Berlin")).toOffsetDateTime()
  }

}
