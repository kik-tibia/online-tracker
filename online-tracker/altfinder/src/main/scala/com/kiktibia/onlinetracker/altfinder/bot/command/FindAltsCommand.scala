package com.kiktibia.onlinetracker.altfinder.bot.command

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*

class FindAltsCommand(service: AltFinderService[IO]) extends Command {

  override val command: SlashCommandData = Commands.slash("alts", "A list of possible alts for a list of characters")
    .addOptions(
      List(
        new OptionData(OptionType.STRING, "characters", "A list of characters to check, comma separated", true, false),
        new OptionData(
          OptionType.STRING,
          "from",
          "The date to start searching from, in YYYY-MM-DD format. Uses Server Save time."
        ),
        new OptionData(
          OptionType.STRING,
          "to",
          "The date to search until, in YYYY-MM-DD format. Uses Server Save time."
        )
      ).asJava
    )

  override def handleEvent(event: SlashCommandInteractionEvent): MessageEmbed = {
    println("in handleEvent")

    val options: List[OptionMapping] = event.getInteraction.getOptions.asScala.toList
    val chars = options.find(_.getName == "characters").get.getAsString()
    val charList = chars.split(",").map(_.trim).toList
    val parseFrom: Either[String, Option[OffsetDateTime]] = parseDateOptionMapping(options, "from")
    val parseTo: Either[String, Option[OffsetDateTime]] = parseDateOptionMapping(options, "to")

    val embedBuilder = (new EmbedBuilder()).setColor(embedColour).setTitle("Alt finder")

    (parseFrom, parseTo) match {
      case (Right(from), Right(to)) =>
        val results = service.findAndPrintAlts(charList, from, to).unsafeRunSync()

        val dateMessage = (from, to) match {
          case (None, None) => "Max range"
          case (None, Some(t)) => s"Until ${t.toLocalDate()}"
          case (Some(f), None) => s"From ${f.toLocalDate()}"
          case (Some(f), Some(t)) => s"From ${f.toLocalDate()} until ${t.toLocalDate()}"
        }

        embedBuilder.addField("Searched characters", results.searchedCharacters.mkString(", "), false)
          .addField("Total logins", results.mainLogins.toString(), false).addField("Date range", dateMessage, false)
          .addField("Possible matches", results.adjacencies.take(20).mkString("\n"), false).build()
      case _ =>
        val errors = List(parseFrom, parseTo).map(_.left.toOption).flatten.mkString("\n")
        embedBuilder.addField("Failed", errors, false).build()

    }
  }

  private def parseDateOptionMapping(
      options: List[OptionMapping],
      name: String
  ): Either[String, Option[OffsetDateTime]] = {
    val option: Option[Either[String, OffsetDateTime]] = options.find(_.getName == name).map(optionMappingToSSDateTime)
    option.map(_.right.map(Some(_))).getOrElse(Right(None))
  }

}
