package com.kiktibia.onlinetracker.repo

import cats.effect.IO
import skunk.*
import skunk.codec.all.*
import skunk.implicits.sql

import java.time.OffsetDateTime

case class CharacterRow(id: Option[Long], name: String, created: OffsetDateTime)
case class CharacterNameHistoryRow(id: Option[Long], characterId: Long, name: String, fromDate: OffsetDateTime, toDate: OffsetDateTime)
case class CurrentlyOnlineRow(id: Option[Long], characterId: Long, worldId: Long, loginTime: Long)

trait OnlineTrackerRepo {
  def createCharacter(character: CharacterRow): IO[Unit]

  def getAllCharacters: IO[List[CharacterRow]]

  def getAllOnline: IO[List[CurrentlyOnlineRow]]
}

class OnlineTrackerRepoImpl(session: Session[IO]) extends OnlineTrackerRepo {

  val characterDecoder: Decoder[CharacterRow] =
    (int8 ~ varchar ~ timestamptz).map {
      case id ~ name ~ created => CharacterRow(Some(id), name, created)
    }

  val characterEncoder: Encoder[CharacterRow] =
    (varchar ~ timestamptz).values.contramap(
      (c: CharacterRow) => (c.name, c.created)
    )

  val onlineDecoder: Decoder[CurrentlyOnlineRow] =
    (int8 ~ int8 ~ int8 ~ int8).map {
      case id ~ characterId ~ worldId ~ loginTime => CurrentlyOnlineRow(Some(id), characterId, worldId, loginTime)
    }

  override def createCharacter(character: CharacterRow): IO[Unit] = {
    val q: Command[CharacterRow] =
      sql"""
           INSERT INTO character(name, created)
           VALUES $characterEncoder
      """
        .command
    session.prepare(q).map(_.execute(character)).void
  }

  override def getAllCharacters: IO[List[CharacterRow]] = {
    val q: Query[Void, CharacterRow] =
      sql"""
           SELECT id, name, created
           FROM character
      """
        .query(characterDecoder)
    session.execute(q)
  }

  override def getAllOnline: IO[List[CurrentlyOnlineRow]] = {
    val q: Query[Void, CurrentlyOnlineRow] =
      sql"""
           SELECT id, character_id, login_time
           FROM online
      """
        .query(onlineDecoder)
    session.execute(q)
  }

}
