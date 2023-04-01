package com.kiktibia.onlinetracker.repo

import java.time.OffsetDateTime

object Model {
  case class WorldRow(id: Long, name: String)

  case class CharacterRow(id: Option[Long], name: String, created: OffsetDateTime)

  case class WorldSaveTimeRow(id: Option[Long], worldId: Long, sequenceId: Long, saveTime: OffsetDateTime)

  case class OnlineNameTime(name: String, loginTime: Long)
}
