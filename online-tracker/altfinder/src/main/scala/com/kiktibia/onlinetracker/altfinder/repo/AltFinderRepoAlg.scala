package com.kiktibia.onlinetracker.altfinder.repo

import com.kiktibia.onlinetracker.altfinder.repo.Model.*

import java.time.OffsetDateTime

trait AltFinderRepoAlg[F[_]] {
  def getOnlineTimes(
      characterNames: List[String],
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime]
  ): F[List[OnlineSegment]]

  def getPossibleMatches(
      characterNames: List[String],
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime],
      distance: Option[Int]
  ): F[List[OnlineSegment]]

  def getCharacterName(characterId: Long): F[String]

  def getCharacterHistories(
      characterName: List[String],
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime]
  ): F[List[OnlineDateSegment]]

  def getPastCharacterNames(characterName: String): F[List[String]]
}
