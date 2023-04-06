package com.kiktibia.onlinetracker.tracker.repo

import cats.effect.IO
import com.kiktibia.onlinetracker.tracker.repo.Model.*

import java.time.OffsetDateTime

trait OnlineTrackerRepo {
  def getWorld(name: String): IO[WorldRow]

  def getLatestSaveTime(worldId: Long): IO[Option[OffsetDateTime]]

  def getAllOnline(worldId: Long): IO[List[OnlineNameTime]]

  def getMaxSequenceId(worldId: Long): IO[Option[Long]]

  def insertWorldSaveTime(w: WorldSaveTimeRow): IO[Long]

  def insertCharacterNameHistory(characterNameHistoryRow: CharacterNameHistoryRow): IO[Unit]

  def updateCharacterName(id: Long, newName: String, time: OffsetDateTime): IO[Unit]

  def insertCharacter(character: CharacterRow): IO[Unit]

  def getCharacter(name: String): IO[Option[CharacterRow]]

  def insertOnline(online: OnlineNameTime, worldId: Long): IO[Unit]

  def deleteOnline(name: String, worldId: Long): IO[Unit]

  def insertOnlineHistory(name: String, loginTime: Long, logoutTime: Long): IO[Unit]
}
