package com.kiktibia.onlinetracker.altfinder.bot.command

import cats.effect.IO
import cats.effect.kernel.Async
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraper
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.MessageEmbed.Field
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters.*

class FindAltsCommand[F[_]: Async](service: AltFinderService[F]) extends Command[F] {

  given Logger[F] = Slf4jLogger.getLogger[F]

  override val command: SlashCommandData = Commands.slash("alts", "A list of possible alts for a list of characters")
    .setGuildOnly(true).addOptions(
      List(
        new OptionData(OptionType.STRING, "characters", "A list of characters to check, comma separated.", true, false),
        new OptionData(
          OptionType.STRING,
          "from",
          "The date to start searching from, in YYYY-MM-DD format. Uses Server Save time."
        ),
        new OptionData(
          OptionType.STRING,
          "to",
          "The date to search until, in YYYY-MM-DD format. Uses Server Save time."
        ),
        new OptionData(
          OptionType.INTEGER,
          "distance",
          "The distance in minutes between log off / log on of two characters to count as a hit. Defaults to 0.",
          false,
          false
        ),
        new OptionData(
          OptionType.BOOLEAN,
          "include-clashes",
          "Enable this to show clashing characters in the results.",
          false,
          false
        )
      ).asJava
    )

  override def handleEvent(event: SlashCommandInteractionEvent): F[MessageEmbed] = {
    val options: List[OptionMapping] = event.getInteraction.getOptions.asScala.toList
    val chars = options.find(_.getName == "characters").get.getAsString()
    val charList = chars.split(",").map(_.trim).toList
    val parseFrom: Either[String, Option[OffsetDateTime]] = parseDateOptionMapping(options, "from")
    val parseTo: Either[String, Option[OffsetDateTime]] = parseDateOptionMapping(options, "to")
    val distance = options.find(_.getName == "distance").map(_.getAsInt())
    val includeClashes = options.find(_.getName == "include-clashes").map(_.getAsBoolean()).getOrElse(false)

    val embedBuilder = (new EmbedBuilder()).setColor(embedColour).setTitle("Alt finder")

    Logger[F].info(event.getUser().getName()) *>
      ((parseFrom, parseTo) match {
        case (Right(from), Right(to)) => service.findAndPrintAlts(charList, from, to, distance, includeClashes)
            .map { results =>

              val dateMessage = (results.searchedFrom, results.searchedTo) match {
                case (None, None) => "Max range"
                case (None, Some(t)) => s"Until ${t.toLocalDate()}"
                case (Some(f), None) => s"From ${f.toLocalDate()}"
                case (Some(f), Some(t)) => s"From ${f.toLocalDate()} until ${t.toLocalDate()}"
              }

              val bazaarScraperError = "Error accessing exevopan.com"
              val tradedField = (results.sales.allSales, results.sales.numberOfErrors) match
                case (Nil, 0) => None
                case (Nil, _) => Some(new Field("Couldn't check if traded", bazaarScraperError, false))
                case (sales, _) =>
                  val salesList = results.sales.characterSales.flatMap { s =>
                    s.saleDates match
                      case Right(Nil) => None
                      case Right(dates) => Some(s"**${s.name}**: ${dates.map(_.toLocalDate).mkString(", ")}")
                      case Left(_) => Some(s"**${s.name}**: $bazaarScraperError")
                  }
                  val dateMessage = from match
                    case None => "Setting the `from` date to be the date of the latest sale."
                    case Some(_) => "Using `from` date provided. Results may be inaccurate."
                  val message = s"The following characters have been traded:\n${salesList.mkString("\n")}\n$dateMessage"
                  Some(new Field("Traded character detected", message, false))

              embedBuilder.addField("Searched characters", results.searchedCharacters.mkString(", "), false)
                .addFieldOption(tradedField).addField("Total logins", results.mainLogins.toString(), true)
                .addField("Date range", dateMessage, true).addField("\u200b", "\u200b", true)
                .addField("Adjacency distance", appendMinutes(distance.getOrElse(0)), true)
                .addField("Include clashes", includeClashes.toString, true).addField("\u200b", "\u200b", true)
                .addField("Possible matches", results.adjacencies.take(20).mkString("\n"), false).build()
            }
        case _ =>
          val errors = List(parseFrom, parseTo).map(_.left.toOption).flatten.mkString("\n")
          Async[F].pure(embedBuilder.addField("Failed", errors, false).build())
      })
  }

  extension (eb: EmbedBuilder)
    private def addFieldOption(field: Option[Field]) = field match
      case Some(f) => eb.addField(f)
      case None => eb

  private def parseDateOptionMapping(
      options: List[OptionMapping],
      name: String
  ): Either[String, Option[OffsetDateTime]] = {
    val option: Option[Either[String, OffsetDateTime]] = options.find(_.getName == name).map(optionMappingToSSDateTime)
    option.map(_.right.map(Some(_))).getOrElse(Right(None))
  }

  private def appendMinutes(i: Int) = if (i == 1) s"$i minute" else s"$i minutes"

}
