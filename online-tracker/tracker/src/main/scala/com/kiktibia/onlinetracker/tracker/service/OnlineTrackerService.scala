package com.kiktibia.onlinetracker.tracker.service

import cats.Applicative
import cats.effect.Sync
import cats.implicits.*
import cats.syntax.all.*
import com.kiktibia.onlinetracker.tracker.repo.Model.*
import com.kiktibia.onlinetracker.tracker.repo.OnlineTrackerRepoAlg
import com.kiktibia.onlinetracker.tracker.tibiadata.TibiaDataClientAlg
import com.kiktibia.onlinetracker.tracker.tibiadata.response.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.OffsetDateTime

class OnlineTrackerService[F[_]: Sync](repo: OnlineTrackerRepoAlg[F], tibiaDataClient: TibiaDataClientAlg[F])(using
    FA: Applicative[F]
) {

  given Logger[F] = Slf4jLogger.getLogger[F]

  def updateDataForWorld(world: String): F[Unit] = {
    for
      _ <- Logger[F].info("--- start ---")
      worldResponse <- tibiaDataClient.getWorld(world)
      tdTime = OffsetDateTime.parse(worldResponse.information.timestamp)

      worldId <- repo.getWorld(world).map(_.id)
      latestSaveTime <- repo.getLatestSaveTime(worldId)

      _ <-
        if latestSaveTime.isEmpty || latestSaveTime.exists(_.isBefore(tdTime)) then
          updateOnlineList(worldId, worldResponse, tdTime)
        else Logger[F].info("Not proceeding, received cached response from TibiaData")

      _ <- Logger[F].info("--- end ---")
    yield ()
  }

  private def updateOnlineList(worldId: Long, worldResponse: WorldResponse, time: OffsetDateTime): F[Unit] = {
    for
      _ <- Logger[F].info(s"Updating online list for $time")
      dbOnlineRows <- repo.getAllOnline(worldId)

      dbOnlineNames = dbOnlineRows.map(_.name)
      tdOnlineNames = worldResponse.world.online_players.getOrElse(Nil).map(_.name)

      loggedOff = dbOnlineNames.filterNot(i => tdOnlineNames.contains(i)).sorted
      loggedOn = tdOnlineNames.filterNot(i => dbOnlineNames.contains(i)).sorted

      lastSequenceId <- repo.getMaxSequenceId(worldId).map(_.getOrElse(0L))
      saveTimeId <- repo.insertWorldSaveTime(WorldSaveTimeRow(None, worldId, lastSequenceId + 1, time))

      _ <- Logger[F].info(s"Inserting ${loggedOn.length} characters: ${loggedOn.mkString(", ")}")
      _ <- loggedOn.map(i => checkIfCharacterExists(i, time)).sequence
      _ <- loggedOn.map(i => repo.insertOnline(OnlineNameTime(i, saveTimeId), worldId)).sequence

      _ <- Logger[F].info(s"Removing ${loggedOff.length} characters: ${loggedOff.mkString(", ")}")
      _ <- loggedOff.map(i => repo.deleteOnline(i, worldId)).sequence

      _ <- dbOnlineRows.filter(i => loggedOff.contains(i.name))
        .map(i => repo.insertOnlineHistory(i.name, i.loginTime, saveTimeId)).sequence
    yield ()
  }

  private def checkIfCharacterExists(name: String, time: OffsetDateTime): F[Unit] = {
    for
      maybeChar <- repo.getCharacter(name)
      _ <- maybeChar match {
        case Some(_) => FA.pure(()) // Character already exists in the database, do nothing
        case None => insertOrRenameCharacter(name, time)
      }
    yield ()
  }

  private def insertOrRenameCharacter(name: String, time: OffsetDateTime): F[Unit] = {
    // Inserts a new character unless if the character is was renamed, in which case handles the rename
    for
      formerNames <- tibiaDataClient.getCharacter(name).map(_.character.character.former_names.getOrElse(Nil))
      formerNamesCharacters <- formerNames.map(i => repo.getCharacter(i)).sequence.map(_.flatten)
      _ <- formerNamesCharacters.headOption match {
        case Some(c) =>
          val charId = c.id.get
          for
            _ <- repo
              .insertCharacterNameHistory(CharacterNameHistoryRow(None, charId, c.name, c.currentNameSince, time))
            _ <- repo.updateCharacterName(charId, name, time)
          yield ()
        case None => repo.insertCharacter(CharacterRow(None, name, time, time))
      }
    yield ()
  }

}
