package com.kiktibia.onlinetracker.altfinder.service

import cats.effect.Async
import cats.effect.kernel.Async
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
  case class CharacterLoginHistory(characterId: Long, segments: Array[OnlineSegment])

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

class AltFinderService[F[_]: Async](repo: AltFinderRepoAlg[F], bazaarScraper: BazaarScraper[F]) {

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
      distance: Option[Int],
      includeClashes: Boolean
  ): F[AltsResults] = {
    for
      _ <- Logger[F].info(s"Searching for: ${characterNames.mkString(", ")}")
      _ <- Logger[F].info(s"Date range: $from - $to")
      pastNames <- characterNames.map(n => repo.getPastCharacterNames(n).map(l => n :: l)).sequence
      salesList <- pastNames.map(bazaarScraper.multipleCharacterSales).sequence.map(CharacterSalesList(_))
      tradedFrom = from.orElse { salesList.latestSale.map(_.toOffsetDateTime()) }
      mainSegments <- repo.getOnlineTimes(characterNames, tradedFrom, to)
      _ <- Logger[F].info(s"Got online times for searched characters (${mainSegments.length} rows)")
      _ <- Logger[F].info(RamUsageEstimator.humanSizeOf(mainSegments))
      matchesToCheck <- repo.getPossibleMatches(characterNames, tradedFrom, to, distance)
      _ <- Logger[F].info("Got online times for possible matched characters")
      _ <- Logger[F].info(RamUsageEstimator.humanSizeOf(matchesToCheck))
      _ <- Logger[F].info(s"${matchesToCheck.length} rows to analyse")
      adj = getAdjacencies(mainSegments, matchesToCheck, includeClashes, distance.getOrElse(0)).take(20)
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
    val characterHistories = others.groupBy(_.characterId).toList
      .map(i => CharacterLoginHistory(i._1, i._2.toArray.sortBy(_.start)))
    val mhArray = mainHistory.toArray.sortBy(_.start)

    characterHistories.flatMap { h =>
      val clashes =
        if (includeClashes) { countClashes(mhArray, h.segments) }
        else {
          val clashes = hasClashes(mhArray, h.segments)
          if (clashes) -1 else 0
        }
      if clashes == 0 || includeClashes then
        Some(CharacterAdjacencies(
          h.characterId,
          None,
          countAdjacencies(mhArray, h.segments, distance),
          clashes,
          h.segments.length
        ))
      else None
    }.sortBy(-_.adjacencies)
  }

  def hasClashes(mainHistory: Array[OnlineSegment], other: Array[OnlineSegment]): Boolean = {
    // Using two sliding pointers to check more efficiently
    var i = 0
    var j = 0
    while (i < mainHistory.length && j < other.length) {
      val mi = mainHistory(i)
      val oj = other(j)

      if (oj.start < mi.end && mi.start < oj.end) return true

      // Increment the earliest pointer, keeping them kind of in sync
      if (mi.end < oj.end) i += 1 else j += 1
    }

    false
  }

  def countClashes(mainHistory: Array[OnlineSegment], other: Array[OnlineSegment]): Int = {
    // Similar to hasClashes (sliding pointers)
    var i = 0
    var j = 0
    var count = 0
    while (i < mainHistory.length && j < other.length) {
      val mi = mainHistory(i)
      val oj = other(j)

      if (oj.start < mi.end && mi.start < oj.end) count += 1

      if (mi.end < oj.end) i += 1 else j += 1
    }

    count
  }

  // Distance is the acceptable distance between logouts and logins.
  // No point optimising this one until the database query is optimised (it takes like 50x longer than this method)
  private def countAdjacencies(mainHistory: Array[OnlineSegment], other: Array[OnlineSegment], distance: Int): Int = {
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
