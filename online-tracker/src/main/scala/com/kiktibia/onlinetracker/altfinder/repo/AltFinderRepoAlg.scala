package com.kiktibia.onlinetracker.altfinder.repo

import com.kiktibia.onlinetracker.altfinder.repo.Model.OnlineSegment

trait AltFinderRepoAlg[F[_]] {
  def getOnlineTimes(characterNames: List[String]): F[List[OnlineSegment]]

  def getPossibleMatches(characterNames: List[String]): F[List[OnlineSegment]]

  def getCharacterName(characterId: Long): F[String]
}
