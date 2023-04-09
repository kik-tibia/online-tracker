package com.kiktibia.onlinetracker.altfinder.repo

import java.time.OffsetDateTime

object Model {
  case class OnlineSegment(characterId: Long, start: Long, end: Long)

  case class OnlineDateSegment(characterName: String, start: OffsetDateTime, end: OffsetDateTime)
}
