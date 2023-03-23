package com.kiktibia.onlinetracker.service

import cats.effect.IO
import com.kiktibia.onlinetracker.repo.{CharacterRow, OnlineTrackerRepo}
import com.kiktibia.onlinetracker.tibiadata.TibiaDataClient
import com.kiktibia.onlinetracker.tibiadata.response.CharacterResponse
import com.typesafe.scalalogging.StrictLogging

import java.time.OffsetDateTime

class OnlineTrackerService(repo: OnlineTrackerRepo, tibiaDataClient: TibiaDataClient) extends StrictLogging {

  def doSomeStuff(): IO[Unit] = {
    logger.info("Doing stuff")
    for {
      w <- tibiaDataClient.getWorld("Nefera")
      t = OffsetDateTime.parse(w.information.timestamp)
      c <- tibiaDataClient.getCharacter("Kikaro")
      _ <- upsertChar(c, t)
      _ <- IO(logger.info("done"))
      //      chars <- repo.getAllOnline("Testworld2")
      //      _ <- IO.println(chars)
      //      _ <- IO.println(w.worlds.world.online_players.length)
      //      _ <- IO.println(c.characters.character.level)
    } yield ()
  }

  def updateOnlineList(world: String): IO[Unit] = {
    for {
      dbOnline <- repo.getAllOnline(world)
      tdOnline <- tibiaDataClient.getWorld(world)
      //      loggedOff = dbOnline.filterNot(_.)
      //      loggedOn = ???
    } yield ()
    ???
  }

  // TODO check if char has renamed and handle that
  def upsertChar(character: CharacterResponse, time: OffsetDateTime): IO[Unit] = {
      repo.createCharacterIfNotExists(CharacterRow(None, character.characters.character.name, time))
  }

}
