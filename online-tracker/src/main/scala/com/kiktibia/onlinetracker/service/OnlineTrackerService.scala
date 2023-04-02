package com.kiktibia.onlinetracker.service

import cats.effect.IO
import cats.implicits.*
import com.kiktibia.onlinetracker.repo.Model.*
import com.kiktibia.onlinetracker.repo.OnlineTrackerRepo
import com.kiktibia.onlinetracker.tibiadata.TibiaDataClient
import com.kiktibia.onlinetracker.tibiadata.response.*
import com.typesafe.scalalogging.StrictLogging

import java.time.OffsetDateTime

class OnlineTrackerService(repo: OnlineTrackerRepo, tibiaDataClient: TibiaDataClient) extends StrictLogging {

  def updateDataForWorld(world: String): IO[Unit] = {
    for {
      _ <- IO.println("--- start ---")
      worldResponse <- tibiaDataClient.getWorld(world)
      tdTime = OffsetDateTime.parse(worldResponse.information.timestamp)

      worldId <- repo.getWorld(world).map(_.id)
      latestSaveTime <- repo.getLatestSaveTime(worldId)

      _ <- if (tdTime != latestSaveTime) updateOnlineList(worldId, worldResponse, tdTime)
      else IO.println("Not proceeding, received cached response from TibiaData")

      _ <- IO.println("--- end ---")
    } yield IO.unit
  }

  private def updateOnlineList(worldId: Long, worldResponse: WorldResponse, time: OffsetDateTime): IO[Unit] = {
    for {
      _ <- IO.println(s"Updating online list for $time")
      dbOnlineRows <- repo.getAllOnline(worldId)

      dbOnlineNames = dbOnlineRows.map(_.name)
      tdOnlineNames = worldResponse.worlds.world.online_players.map(_.name)
      _ <- IO.println(s"dbOnlineNames length: ${dbOnlineNames.length}")
      _ <- IO.println(s"tdOnlineNames length: ${tdOnlineNames.length}")

      loggedOff = dbOnlineNames.filterNot(i => tdOnlineNames.contains(i))
      loggedOn = tdOnlineNames.filterNot(i => dbOnlineNames.contains(i))

      lastSequenceId <- repo.getMaxSequenceId(worldId)
      saveTimeId <- repo.insertWorldSaveTime(WorldSaveTimeRow(None, worldId, lastSequenceId + 1, time))
      _ <- IO.println(s"Inserted save time row with ID $saveTimeId and sequence ID ${lastSequenceId + 1}")

      _ <- IO.println(s"Inserting ${loggedOn.length} characters to online list")
      _ <- IO.println(loggedOn.mkString(", "))

      _ <- loggedOn.map(i => upsertChar(i, time)).sequence
      _ <- loggedOn.map(i => repo.insertOnline(OnlineNameTime(i, saveTimeId), worldId)).sequence

      _ <- IO.println(s"Removing ${loggedOff.length} characters from online list")
      _ <- IO.println(loggedOff.mkString(", "))
      _ <- loggedOff.map(i => repo.deleteOnline(i, worldId)).sequence

      _ <- dbOnlineRows.filter(i => loggedOff.contains(i.name))
        .map(i => repo.insertOnlineHistory(i.name, i.loginTime, saveTimeId)).sequence
    } yield IO.unit
  }

  // TODO check if char has renamed and handle that
  private def upsertChar(name: String, time: OffsetDateTime): IO[Unit] = {
    repo.createCharacterIfNotExists(CharacterRow(None, name, time))
  }

}
