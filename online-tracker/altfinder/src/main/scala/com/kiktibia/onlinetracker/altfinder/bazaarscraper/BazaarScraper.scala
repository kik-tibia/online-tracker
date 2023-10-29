package com.kiktibia.onlinetracker.altfinder.bazaarscraper

import cats.effect.Sync
import cats.implicits.*
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraper.CharacterSales
import org.jsoup.Jsoup

import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import scala.jdk.CollectionConverters.*
import java.util.Locale

object BazaarScraper {
  case class CharacterSales(name: String, saleDates: List[LocalDate])

  def latestSale(sales: List[CharacterSales]): Option[LocalDate] = sales.flatMap(_.saleDates).maxOption
}

class BazaarScraper[F[_]: Sync](client: BazaarScraperClientAlg[F]) {

  def characterSales(name: String): F[CharacterSales] = {
    for
      html <- client.searchCharacter(name)
      dates = parseHtml(html)
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
