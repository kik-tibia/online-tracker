package com.kiktibia.onlinetracker.altfinder.repo

import cats.effect.IO
import com.kiktibia.onlinetracker.altfinder.repo.Model.*
import com.kiktibia.onlinetracker.repo.SkunkExtensions
import skunk.codec.all.{int8, timestamptz, varchar}
import skunk.implicits.{sql, toIdOps}
import skunk.*

class AltFinderRepoImpl(val session: Session[IO]) extends AltFinderRepo with AltFinderCodecs with SkunkExtensions {

  override def getCharacterId(characterName: String): IO[Long] = {
    val q: Query[String, Long] =
      sql"""
           SELECT id FROM character
           WHERE lower(name) = lower($varchar)
      """
        .query(int8)
    session.unique(q, characterName)
  }

  override def getOnlineTimes(characterId: Long): IO[List[OnlineSegment]] = {
    val q: Query[Long, OnlineSegment] =
      sql"""
           SELECT o.character_id, o.login_time, o.logout_time
           FROM online_history o
           WHERE character_id = $int8
      """
        .query(onlineSegmentDecoder)
    prepareToList(q, characterId)
  }

  override def getPossibleMatches(characterName: String): IO[List[OnlineSegment]] = {
    val q: Query[String, OnlineSegment] =
      sql"""
          SELECT o3.character_id, o3.login_time, o3.logout_time
          FROM online_history o1 CROSS JOIN online_history o2
          JOIN character ON o1.character_id = character.id
          JOIN online_history o3 on o2.character_id = o3.character_id
          WHERE lower(character.name) = lower($varchar)
          AND o1.login_time >= o2.logout_time
          AND o1.login_time < o2.logout_time + 2;
     """
        .query(onlineSegmentDecoder)
    prepareToList(q, characterName)
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
