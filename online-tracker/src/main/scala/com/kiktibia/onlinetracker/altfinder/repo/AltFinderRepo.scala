package com.kiktibia.onlinetracker.altfinder.repo

import cats.effect.IO
import com.kiktibia.onlinetracker.altfinder.repo.Model.OnlineSegment

trait AltFinderRepo {
  def getOnlineTimes(characterName: String): IO[List[OnlineSegment]]
}
