package com.kiktibia.onlinetracker.altfinder.service

import cats.effect.{IO, Sync}
import cats.implicits.*
import com.kiktibia.onlinetracker.altfinder.repo.AltFinderRepo
import com.kiktibia.onlinetracker.altfinder.repo.Model.OnlineSegment
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class AltFinderService(repo: AltFinderRepo) {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  private case class CharacterHistory(characterId: Long, segments: List[OnlineSegment])

  private case class CharacterIdAdjacencies(characterId: Long, adjacencies: Int)

  private case class CharacterAdjacencies(characterName: String, adjacencies: Int)

  def printOnlineTimes(characterNames: List[String]): IO[Unit] = {
    for {
      mainSegments <- repo.getOnlineTimes(characterNames)
      matchesToCheck <- repo.getPossibleMatches(characterNames)
      _ <- Logger[IO].info(s"${matchesToCheck.length} rows to analyse")
      adj = getAdjacencies(mainSegments, matchesToCheck).take(10)
      results <- adj.map(a => repo.getCharacterName(a.characterId).map(i => CharacterAdjacencies(i, a.adjacencies))).sequence
      _ <- results.map(i => Logger[IO].info(i.toString)).sequence
    } yield IO.unit
  }

  private def getAdjacencies(mainHistory: List[OnlineSegment], others: List[OnlineSegment]): List[CharacterIdAdjacencies] = {
    val characterHistories = others.groupBy(_.characterId).toList.map(i => CharacterHistory(i._1, i._2))
    characterHistories.filter(h => countClashes(mainHistory, h.segments) == 0)
      .map(h => CharacterIdAdjacencies(h.characterId, countAdjacencies(mainHistory, h.segments)))
      .sortBy(-_.adjacencies)
  }

  private def countClashes(mainHistory: List[OnlineSegment], other: List[OnlineSegment]): Int = {
    mainHistory.count { m =>
      other.exists { o =>
        (o.start >= m.start && o.start < m.end) ||
          (o.end > m.start && o.end < m.end)
      }
    }
  }

  private def countAdjacencies(mainHistory: List[OnlineSegment], other: List[OnlineSegment]): Int = {
    mainHistory.count(m => other.exists(o => o.start == m.end)) +
      mainHistory.count(m => other.exists(o => m.start == o.end))
  }

}
