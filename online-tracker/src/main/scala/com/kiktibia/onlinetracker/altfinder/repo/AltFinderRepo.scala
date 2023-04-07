package com.kiktibia.onlinetracker.altfinder.repo

import cats.effect.IO
import com.kiktibia.onlinetracker.altfinder.repo.Model.OnlineSegment

trait AltFinderRepo {
  def getOnlineTimes(characterNames: List[String]): IO[List[OnlineSegment]]

  def getPossibleMatches(characterNames: List[String]): IO[List[OnlineSegment]]

  def getCharacterName(characterId: Long): IO[String]
}
