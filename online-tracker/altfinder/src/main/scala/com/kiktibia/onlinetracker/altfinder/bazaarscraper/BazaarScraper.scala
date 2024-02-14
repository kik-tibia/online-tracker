package com.kiktibia.onlinetracker.altfinder.bazaarscraper

import cats.effect.Sync
import cats.implicits.*
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraper.*
import org.jsoup.Jsoup

import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import scala.jdk.CollectionConverters.*
import java.util.Locale

object BazaarScraper {
  case class BazaarScraperError(message: String)

  case class CharacterSales(name: String, saleDates: Either[BazaarScraperError, List[LocalDate]])

  case class CharacterSalesList(characterSales: List[CharacterSales]) {
    def latestSale: Option[LocalDate] = characterSales.map(_.saleDates).flatMap(_.toOption).flatten.maxOption
    def allSales: List[LocalDate] = characterSales.flatMap(_.saleDates.getOrElse(Nil))
    def numberOfErrors: Int = characterSales.count(_.saleDates.isLeft)
  }

  def latestSale(sales: List[CharacterSales]): Option[LocalDate] = sales.map(_.saleDates).flatMap(_.toOption).flatten
    .maxOption
}

class BazaarScraper[F[_]: Sync](client: BazaarScraperClientAlg[F]) {

  def characterSales(name: String): F[CharacterSales] = {
    for
      html <- client.searchCharacter(name).map(Right(_)).handleError { case e =>
        Left(BazaarScraperError(e.getMessage()))
      }
      dates = html.map(parseHtml)
    yield CharacterSales(name, dates)
  }

  private def parseHtml(s: String): List[LocalDate] = {
    val resultsTable = Jsoup.parse(s).getElementById("players")
    if (resultsTable == null) List.empty
    else {
      resultsTable.select("tr[role=row]").asScala.toList.tail.filter { tr =>
        tr.selectXpath("./td[5]//div[not(.//div)]").text().trim == "sold"
      }.map { tr =>
        val formatter = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM dd yyyy, HH:mm:ss")
          .toFormatter().withLocale(Locale.US)
        val dateString = tr.selectXpath("./td[4]/span").text()
        LocalDate.parse(dateString, formatter)
      }
    }
  }

}
