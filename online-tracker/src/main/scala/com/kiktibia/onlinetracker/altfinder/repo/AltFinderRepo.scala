package com.kiktibia.onlinetracker.altfinder.repo

import cats.effect.IO
import com.kiktibia.onlinetracker.altfinder.repo.Model.OnlineSegment

trait AltFinderRepo {
  def getCharacterId(characterName: String): IO[Long]

  def getOnlineTimes(characterId: Long): IO[List[OnlineSegment]]

  def getPossibleMatches(characterName: String): IO[List[OnlineSegment]]

  def getCharacterName(characterId: Long): IO[String]
}
