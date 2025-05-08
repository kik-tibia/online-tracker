package com.kiktibia.onlinetracker.altfinder.bot.command

import cats.effect.unsafe.IORuntime
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

trait Command[F[_]] {

  given IORuntime = IORuntime.global

  val command: SlashCommandData
  val embedColour = 16763922

  def handleEvent(event: SlashCommandInteractionEvent): F[MessageEmbed]

  def optionMappingToSSDateTime(option: OptionMapping): Either[String, OffsetDateTime] = {
    val dateString = option.getAsString()
    Try(LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)).toEither.left
      .map(_ => s"Date needs to be in YYYY-MM-DD format. Could not parse date: $dateString")
      .map(d => ZonedDateTime.of(d, LocalTime.of(10, 0), ZoneId.of("Europe/Berlin")).toOffsetDateTime())
  }

}
