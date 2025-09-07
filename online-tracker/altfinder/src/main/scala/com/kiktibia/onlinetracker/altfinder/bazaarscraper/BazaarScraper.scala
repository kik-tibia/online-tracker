package com.kiktibia.onlinetracker.altfinder.bazaarscraper

import cats.effect.Sync
import cats.implicits.*
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraper.*
import io.circe.*
import io.circe.parser.*
import org.jsoup.Jsoup

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import scala.jdk.CollectionConverters.*

object BazaarScraper {
  case class BazaarScraperError(message: String)

  case class CharacterSales(name: String, saleDates: Either[BazaarScraperError, List[ZonedDateTime]])

  case class CharacterSalesList(characterSales: List[CharacterSales]) {
    def latestSale: Option[ZonedDateTime] = characterSales.map(_.saleDates).flatMap(_.toOption).flatten.maxOption
    def allSales: List[ZonedDateTime] = characterSales.flatMap(_.saleDates.getOrElse(Nil))
    def numberOfErrors: Int = characterSales.count(_.saleDates.isLeft)
  }

  def latestSale(sales: List[CharacterSales]): Option[ZonedDateTime] = sales.map(_.saleDates).flatMap(_.toOption)
    .flatten.maxOption
}

class BazaarScraper[F[_]: Sync](client: BazaarScraperClientAlg[F]) {
  private val zone = ZoneId.of("Europe/Berlin")

  def multipleCharacterSales(names: List[String]): F[CharacterSales] = {
    for
      allSales <- names.map(singleCharacterSales).sequence
      dates = allSales.flatMap(_.saleDates.getOrElse(Nil))
      errored = allSales.map(_.saleDates).find(_.isLeft)
    yield CharacterSales(allSales.head.name, errored.getOrElse(Right(dates)))
  }

  private def singleCharacterSales(name: String): F[CharacterSales] = {
    for
      json <- client.searchCharacter(name).map(Right(_)).handleError { case e =>
        Left(BazaarScraperError(e.getMessage()))
      }
      dates = json.flatMap(h => parseJson(h, name))
    yield CharacterSales(name, dates.map(_.map(instantToSSDay)))
  }

  private def instantToSSDay(instant: Instant): ZonedDateTime = {
    val zdtAtInstant = instant.atZone(zone)
    val serverSaveSameDay = zdtAtInstant.toLocalDate.atTime(LocalTime.of(10, 0)).atZone(zone)
    val ssDay = if (serverSaveSameDay.toInstant.isBefore(instant)) serverSaveSameDay.plusDays(1) else serverSaveSameDay
    ssDay.minusDays(1) // workaround for exevo pan bug https://github.com/xandjiji/exevo-pan/issues/241
  }

  private def parseJson(jsonString: String, name: String): Either[BazaarScraperError, List[Instant]] = {
    parse(jsonString).leftMap(err => BazaarScraperError(s"Invalid JSON: ${err.getMessage}")).flatMap { json =>
      val pageCur = json.hcursor.downField("page")
      pageCur.as[List[Json]] match
        case Left(decErr) => Left(BazaarScraperError(s"Invalid 'page' field: ${decErr.getMessage}"))
        case Right(items) =>
          try
            val dates = items.flatMap { item =>
              val c = item.hcursor
              val nickOpt = c.get[String]("nickname").toOption
              val endOpt = c.get[Long]("auctionEnd").toOption
              val biddedOpt = c.get[Boolean]("hasBeenBidded").toOption
              (nickOpt, endOpt, biddedOpt) match
                case (Some(nick), Some(epoch), Some(bidded)) if nick == name && bidded =>
                  Some(Instant.ofEpochSecond(epoch))
                case _ => None
            }
            Right(dates)
          catch
            case e: Exception => Left(BazaarScraperError(s"Unexpected error while converting dates: ${e.getMessage}"))
    }
  }
}
