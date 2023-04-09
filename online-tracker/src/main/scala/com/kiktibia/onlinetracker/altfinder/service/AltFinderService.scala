package com.kiktibia.onlinetracker.altfinder.service

import cats.effect.Sync
import cats.implicits.*
import com.kiktibia.onlinetracker.altfinder.repo.AltFinderRepoAlg
import com.kiktibia.onlinetracker.altfinder.repo.Model.OnlineSegment
import com.kiktibia.onlinetracker.altfinder.service.AltFinderService.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object AltFinderService {
  case class CharacterLoginHistory(characterId: Long, segments: List[OnlineSegment])

  case class CharacterAdjacencies
    (characterId: Long, characterName: Option[String], adjacencies: Int, clashes: Int, logins: Int) {
    override def toString: String = s"${characterName.getOrElse("")}: $adjacencies / $clashes / $logins"
  }

}

class AltFinderService[F[_]: Sync](repo: AltFinderRepoAlg[F]) {

  implicit private def logger: Logger[F] = Slf4jLogger.getLogger[F]

  def printLoginHistories(characterNames: List[String]): F[Unit] = {
    for
      history <- repo.getCharacterHistories(characterNames)
      _ <- history.map(i => Logger[F].info(i.toString)).sequence
    yield ()
  }

  def findAndPrintAlts(characterNames: List[String]): F[Unit] = {
    for
      mainSegments <- repo.getOnlineTimes(characterNames)
      matchesToCheck <- repo.getPossibleMatches(characterNames)
      _ <- Logger[F].info(s"${matchesToCheck.length} rows to analyse")
      adj = getAdjacencies(mainSegments, matchesToCheck).take(20)
      results <- adj.map(a => repo.getCharacterName(a.characterId).map { i =>
        a.copy(characterName = Some(i))
      }).sequence
      _ <- results.map(i => Logger[F].info(i.toString)).sequence
    yield ()
  }

  private def getAdjacencies(mainHistory: List[OnlineSegment], others: List[OnlineSegment]): List[CharacterAdjacencies] = {
    val characterHistories = others.groupBy(_.characterId).toList.map(i => CharacterLoginHistory(i._1, i._2))
    characterHistories.filter(h => countClashes(mainHistory, h.segments) == 0)
      .map(h => CharacterAdjacencies(h.characterId, None, countAdjacencies(mainHistory, h.segments), 0, h.segments.length))
      .sortBy(-_.adjacencies)
  }

  private def countClashes(mainHistory: List[OnlineSegment], other: List[OnlineSegment]): Int = {
    mainHistory.count { m =>
      other.exists { o =>
        (o.start >= m.start && o.start < m.end) ||
          (o.end > m.start && o.end < m.end) ||
          (o.start < m.start && o.end >= m.end)
      }
    }
  }

  private def countAdjacencies(mainHistory: List[OnlineSegment], other: List[OnlineSegment]): Int = {
    mainHistory.count(m => other.exists(o => o.start == m.end)) +
      mainHistory.count(m => other.exists(o => m.start == o.end))
  }

}
