package com.kiktibia.onlinetracker.service

import cats.effect.IO
import com.kiktibia.onlinetracker.repo.OnlineTrackerRepo
import com.kiktibia.onlinetracker.tibiadata.TibiaDataClient

class OnlineTrackerService(repo: OnlineTrackerRepo, tibiaDataClient: TibiaDataClient) {

  def doSomeStuff(): IO[Unit] = {
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
