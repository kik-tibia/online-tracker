package com.kiktibia.onlinetracker.altfinder.repo

import cats.effect.IO
import com.kiktibia.onlinetracker.altfinder.repo.Model.*
import com.kiktibia.onlinetracker.repo.SkunkExtensions
import skunk.codec.all.{int8, timestamptz, varchar}
import skunk.implicits.{sql, toIdOps}
import skunk.*

class AltFinderRepoImpl(val session: Session[IO]) extends AltFinderRepo with AltFinderCodecs with SkunkExtensions {

  override def getOnlineTimes(characterName: String): IO[List[OnlineSegment]] = {
    val q: Query[String, OnlineSegment] =
      sql"""
           SELECT online_history.login_time, online_history.logout_time
           FROM online_history
           JOIN character on online_history.character_id = character.id
           WHERE character.name = $varchar
      """
        .query(onlineSegmentDecoder)
    prepareToList(q, characterName)
  }

}
