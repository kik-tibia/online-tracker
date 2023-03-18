package com.kiktibia.onlinetracker.service

import cats.effect.IO
import com.kiktibia.onlinetracker.repo.OnlineTrackerRepo
import com.kiktibia.onlinetracker.tibiadata.TibiaDataClient
import com.typesafe.scalalogging.StrictLogging

class OnlineTrackerService(repo: OnlineTrackerRepo, tibiaDataClient: TibiaDataClient) extends StrictLogging {

  def doSomeStuff(): IO[Unit] = {
    logger.info("Doing stuff")
    for {
      w <- tibiaDataClient.getWorld("Nefera")
      c <- tibiaDataClient.getCharacter("Kikaro")
      chars <- repo.getAllCharacters
      _ <- IO.println(w.worlds.world.online_players.length)
      _ <- IO.println(c.characters.character.level)
      _ <- IO.println(chars)
    } yield ()
  }

}
