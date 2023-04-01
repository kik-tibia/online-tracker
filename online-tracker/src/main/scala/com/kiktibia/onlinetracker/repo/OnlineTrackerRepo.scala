package com.kiktibia.onlinetracker.repo

import cats.effect.IO
import com.kiktibia.onlinetracker.repo.Model.*

import java.time.OffsetDateTime

trait OnlineTrackerRepo {
  def getWorld(name: String): IO[WorldRow]

  def getLatestSaveTime(worldId: Long): IO[OffsetDateTime]

  def getAllOnline(worldId: Long): IO[List[OnlineNameTime]]

  def getMaxSequenceId(worldId: Long): IO[Long]

  def insertWorldSaveTime(w: WorldSaveTimeRow): IO[Long]

  def createCharacterIfNotExists(character: CharacterRow): IO[Unit]

  def insertOnline(online: OnlineNameTime, worldId: Long): IO[Unit]

  def deleteOnline(name: String, worldId: Long): IO[Unit]
}
