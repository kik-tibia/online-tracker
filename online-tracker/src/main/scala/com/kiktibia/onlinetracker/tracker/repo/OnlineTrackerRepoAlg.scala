package com.kiktibia.onlinetracker.tracker.repo

import com.kiktibia.onlinetracker.tracker.repo.Model.*

import java.time.OffsetDateTime

trait OnlineTrackerRepoAlg[F[_]] {
  def getWorld(name: String): F[WorldRow]

  def getLatestSaveTime(worldId: Long): F[Option[OffsetDateTime]]

  def getAllOnline(worldId: Long): F[List[OnlineNameTime]]

  def getMaxSequenceId(worldId: Long): F[Option[Long]]

  def insertWorldSaveTime(w: WorldSaveTimeRow): F[Long]

  def insertCharacterNameHistory(characterNameHistoryRow: CharacterNameHistoryRow): F[Unit]

  def updateCharacterName(id: Long, newName: String, time: OffsetDateTime): F[Unit]

  def insertCharacter(character: CharacterRow): F[Unit]

  def getCharacter(name: String): F[Option[CharacterRow]]

  def insertOnline(online: OnlineNameTime, worldId: Long): F[Unit]

  def deleteOnline(name: String, worldId: Long): F[Unit]

  def insertOnlineHistory(name: String, loginTime: Long, logoutTime: Long): F[Unit]
}
