package com.kiktibia.onlinetracker.altfinder.service

import cats.effect.Sync
import cats.implicits.*
import com.carrotsearch.sizeof.RamUsageEstimator
import com.kiktibia.onlinetracker.altfinder.LoginPlotter
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraper
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraper.*
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraperClientAlg
import com.kiktibia.onlinetracker.altfinder.repo.AltFinderRepoAlg
import com.kiktibia.onlinetracker.altfinder.repo.Model.OnlineDateSegment
import com.kiktibia.onlinetracker.altfinder.repo.Model.OnlineSegment
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AltFinderService {
  case class CharacterLoginHistory(characterId: Long, segments: List[OnlineSegment])

  case class CharacterAdjacencies(
      characterId: Long,
      characterName: Option[String],
      adjacencies: Int,
      clashes: Int,
      logins: Int
  ) {
    override def toString: String = s"${characterName.getOrElse("")}: $adjacencies / $clashes / $logins"
  }

  case class AltsResults(
      searchedCharacters: List[String],
      searchedFrom: Option[OffsetDateTime],
      searchedTo: Option[OffsetDateTime],
      mainLogins: Int,
      adjacencies: List[CharacterAdjacencies],
      sales: CharacterSalesList
  )

}

class AltFinderService[F[_]: Sync](repo: AltFinderRepoAlg[F], bazaarScraper: BazaarScraper[F]) {

  given Logger[F] = Slf4jLogger.getLogger[F]

  def onlineHistories(
      characterNames: List[String],
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime]
  ): F[List[OnlineDateSegment]] = {
    for
      history <- repo.getCharacterHistories(characterNames, from, to)
      _ <- history.map(i => Logger[F].info(i.toString)).sequence
    // _ = LoginPlotter.plot(history)
    yield history
  }

  def findAndPrintAlts(
      characterNames: List[String],
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime],
      distance: Option[Int]
  ): F[AltsResults] = {
    for
      _ <- Logger[F].info(s"Searching for: ${characterNames.mkString(", ")}")
      _ <- Logger[F].info(s"Date range: $from - $to")
      salesList <- characterNames.map(bazaarScraper.characterSales).sequence.map(CharacterSalesList(_))
      tradedFrom = from.orElse {
        salesList.latestSale
          .map(l => ZonedDateTime.of(l, LocalTime.of(10, 0), ZoneId.of("Europe/Berlin")).toOffsetDateTime())
      }
      mainSegments <- repo.getOnlineTimes(characterNames, tradedFrom, to)
      _ <- Logger[F].info(s"Got online times for searched characters (${mainSegments.length} rows)")
      _ <- Logger[F].info(RamUsageEstimator.humanSizeOf(mainSegments))
      matchesToCheck <- repo.getPossibleMatches(characterNames, tradedFrom, to, distance)
      _ <- Logger[F].info("Got online times for possible matched characters")
      _ <- Logger[F].info(RamUsageEstimator.humanSizeOf(matchesToCheck))
      _ <- Logger[F].info(s"${matchesToCheck.length} rows to analyse")
      adj = getAdjacencies(mainSegments, matchesToCheck, includeClashes = false, distance.getOrElse(0)).take(20)
      results <- adj.map(a => repo.getCharacterName(a.characterId).map { i => a.copy(characterName = Some(i)) })
        .sequence
      altsResults = AltsResults(characterNames, tradedFrom, to, mainSegments.length, results, salesList)
      _ <- results.map(i => Logger[F].info(i.toString)).sequence
    yield altsResults
  }

  def checkForClashes(
      characterNames: List[String],
      toCheck: List[String],
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime]
  ): F[Unit] = {
    for
      mainSegments <- repo.getOnlineTimes(characterNames, from, to)
      toCheckSegments <- repo.getOnlineTimes(toCheck, from, to)
      _ <- Logger[F].info(s"${toCheckSegments.length} rows to analyse from ${mainSegments.length} segments")
      adj = getAdjacencies(mainSegments, toCheckSegments, includeClashes = true, distance = 0)
      results <- adj.map(a => repo.getCharacterName(a.characterId).map { i => a.copy(characterName = Some(i)) })
        .sequence
      _ <- results.map(i => Logger[F].info(i.toString)).sequence
    yield ()
  }

  private def getAdjacencies(
      mainHistory: List[OnlineSegment],
      others: List[OnlineSegment],
      includeClashes: Boolean,
      distance: Int
  ): List[CharacterAdjacencies] = {
    val characterHistories = others.groupBy(_.characterId).toList.map(i => CharacterLoginHistory(i._1, i._2))

    characterHistories.flatMap { h =>
      val clashes =
        if (includeClashes) { countClashes(mainHistory, h.segments) }
        else {
          val clashes = hasClashes(mainHistory, h.segments)
          if (clashes) -1 else 0
        }
      if clashes == 0 || includeClashes then
        Some(CharacterAdjacencies(
          h.characterId,
          None,
          countAdjacencies(mainHistory, h.segments, distance),
          clashes,
          h.segments.length
        ))
      else None
    }.sortBy(-_.adjacencies)
  }

  private def hasClashes(mainHistory: List[OnlineSegment], other: List[OnlineSegment]): Boolean = {
    // Set to 1 for for an acceptable overlap of 1 minute
    // e.g. if you x-log then switch account, both chars could be online at the same time for 1 minute
    val overlap = 0
    mainHistory.exists { m => other.exists { o => o.start < m.end - overlap && m.start < o.end - overlap } }
  }

  private def countClashes(mainHistory: List[OnlineSegment], other: List[OnlineSegment]): Int = {
    // Set to 1 for for an acceptable overlap of 1 minute
    // e.g. if you x-log then switch account, both chars could be online at the same time for 1 minute
    val overlap = 0
    mainHistory.count { m => other.exists { o => o.start < m.end - overlap && m.start < o.end - overlap } }
  }

  // Distance is the acceptable distance between logouts and logins.
  private def countAdjacencies(mainHistory: List[OnlineSegment], other: List[OnlineSegment], distance: Int): Int = {
    mainHistory.count { m =>
      other.exists { o =>
        val diff = o.start - m.end
        diff >= 0 && diff <= distance
      }
    } + mainHistory.count { m =>
      other.exists { o =>
        val diff = m.start - o.end
        diff >= 0 && diff <= distance
      }
    }
  }

}
