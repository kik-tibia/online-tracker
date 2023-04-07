package com.kiktibia.onlinetracker.altfinder.repo

import cats.effect.IO
import com.kiktibia.onlinetracker.altfinder.repo.Model.*
import com.kiktibia.onlinetracker.repo.SkunkExtensions
import skunk.codec.all.{int8, timestamptz, varchar}
import skunk.implicits.{sql, toIdOps}
import skunk.*

class AltFinderRepoImpl(val session: Session[IO]) extends AltFinderRepo with AltFinderCodecs with SkunkExtensions {

  override def getOnlineTimes(characterNames: List[String]): IO[List[OnlineSegment]] = {
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

  override def getPossibleMatches(characterNames: List[String]): IO[List[OnlineSegment]] = {
    val q: Query[List[String], OnlineSegment] =
      sql"""
          SELECT o3.character_id, o3.login_time, o3.logout_time
          FROM online_history o1 CROSS JOIN online_history o2
          JOIN character ON o1.character_id = character.id
          JOIN online_history o3 ON o2.character_id = o3.character_id
          WHERE LOWER(character.name) IN (${varchar.values.list(characterNames.length)})
          AND o1.login_time >= o2.logout_time
          AND o1.login_time < o2.logout_time + 2;
     """
        .query(onlineSegmentDecoder)
    prepareToList(q, characterNames.map(_.toLowerCase))
  }

  override def getCharacterName(characterId: Long): IO[String] = {
    val q: Query[Long, String] =
      sql"""
           SELECT name FROM character
           WHERE id = $int8
      """
        .query(varchar)
    session.unique(q, characterId)
  }

}
