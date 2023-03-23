package com.kiktibia.onlinetracker.repo

import cats.effect.IO
import skunk.*
import skunk.codec.all.*
import skunk.implicits.{sql, toIdOps}

import java.time.OffsetDateTime

case class CharacterRow(id: Option[Long], name: String, created: OffsetDateTime)

case class CharacterNameHistoryRow(id: Option[Long], characterId: Long, name: String, fromDate: OffsetDateTime, toDate: OffsetDateTime)

case class CurrentlyOnlineRow(id: Option[Long], characterId: Long, worldId: Long, loginTime: Long)

case class OnlineUseful(id: Long, name: String, loginTime: Long)

case class WorldSaveTimeRow(id: Option[Long], worldId: Long, sequenceId: Long, saveTime: OffsetDateTime)

trait OnlineTrackerRepo {
  def createCharacterIfNotExists(character: CharacterRow): IO[Unit]

  def getAllCharacters: IO[List[CharacterRow]]

  def getAllOnline(world: String): IO[List[CurrentlyOnlineRow]]
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

  override def createCharacterIfNotExists(character: CharacterRow): IO[Unit] = {
    val q: Command[CharacterRow] =
      sql"""
           INSERT INTO character(name, created)
           VALUES $characterEncoder
           ON CONFLICT DO NOTHING
      """
        .command
    session.prepare(q).flatMap(_.execute(character)).void
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

  override def getAllOnline(world: String): IO[List[CurrentlyOnlineRow]] = {
    val q: Query[String, CurrentlyOnlineRow] =
      sql"""
           SELECT currently_online.id, currently_online.character_id, currently_online.world_id, currently_online.login_time
           FROM currently_online join world
           ON currently_online.world_id = world.id
           WHERE world.name = $varchar
      """
        .query(onlineDecoder)

    prepareToList(q, world)
  }

  private def prepareToList[A, B](q: Query[A, B], args: A): IO[List[B]] =
    session.prepare(q).flatMap(_.stream(args, 64).compile.toList)

}
