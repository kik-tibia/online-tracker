package com.kiktibia.onlinetracker.altfinder.repo

import cats.Monad
import cats.effect.kernel.Concurrent
import com.kiktibia.onlinetracker.altfinder.repo.Model.*
import com.kiktibia.onlinetracker.repo.SkunkExtensions
import skunk.*
import skunk.codec.all.{int8, timestamptz, varchar}
import skunk.implicits.{sql, toIdOps}

class AltFinderSkunkRepo[F[_]: Monad](val session: Session[F])(implicit val FC: Concurrent[F], val FM: Monad[F])
  extends AltFinderRepoAlg[F] with AltFinderCodecs with SkunkExtensions[F] {

  override def getOnlineTimes(characterNames: List[String]): F[List[OnlineSegment]] = {
    val q: Query[List[String], OnlineSegment] =
      sql"""
           SELECT o.character_id, o.login_time, o.logout_time
           FROM online_history o JOIN character c
           ON o.character_id = c.id
           WHERE LOWER(c.name) IN (${varchar.values.list(characterNames.length)})
      """
        .query(onlineSegmentDecoder)
    prepareToList(q, characterNames.map(_.toLowerCase))
  }

  override def getPossibleMatches(characterNames: List[String]): F[List[OnlineSegment]] = {
    val q: Query[List[String], OnlineSegment] =
      sql"""
          SELECT DISTINCT o3.character_id, o3.login_time, o3.logout_time
          FROM online_history o1
          JOIN online_history o2 ON (o1.login_time = o2.logout_time OR o1.logout_time = o2.login_time)
          JOIN character ON o1.character_id = character.id
          JOIN online_history o3 ON o2.character_id = o3.character_id
          WHERE LOWER(character.name) IN (${varchar.values.list(characterNames.length)})
     """
        .query(onlineSegmentDecoder)
    prepareToList(q, characterNames.map(_.toLowerCase))
  }

  override def getCharacterName(characterId: Long): F[String] = {
    val q: Query[Long, String] =
      sql"""
           SELECT name FROM character
           WHERE id = $int8
      """
        .query(varchar)
    session.unique(q, characterId)
  }

  override def getCharacterHistories(characterNames: List[String]): F[List[OnlineDateSegment]] = {
    val q: Query[List[String], OnlineDateSegment] =
      sql"""
            SELECT c.name, w1.time, w2.time
            FROM online_history o
            JOIN character c ON o.character_id = c.id
            JOIN world_save_time w1 ON o.login_time = w1.id
            JOIN world_save_time w2 ON o.logout_time = w2.id
            WHERE lower(c.name) IN (${varchar.values.list(characterNames.length)})
            ORDER BY w1.sequence_id
      """.query(onlineDateSegmentDecoder)
    prepareToList(q, characterNames.map(_.toLowerCase))
  }

}
