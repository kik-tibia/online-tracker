package com.kiktibia.onlinetracker.tracker.repo

import java.time.OffsetDateTime

object Model {
  case class WorldRow(id: Long, name: String)

  case class CharacterNameHistoryRow(id: Option[Long], characterId: Long, name: String, fromDate: OffsetDateTime, toDate: OffsetDateTime)

  case class CharacterRow(id: Option[Long], name: String, created: OffsetDateTime, currentNameSince: OffsetDateTime)

  case class WorldSaveTimeRow(id: Option[Long], worldId: Long, sequenceId: Long, saveTime: OffsetDateTime)

  case class OnlineNameTime(name: String, loginTime: Long)
}
