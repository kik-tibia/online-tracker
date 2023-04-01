package com.kiktibia.onlinetracker.repo

import cats.effect.IO
import com.kiktibia.onlinetracker.repo.Model.*
import skunk.codec.all.{int8, timestamptz, varchar}
import skunk.implicits.{sql, toIdOps}
import skunk.*

import java.time.OffsetDateTime

class OnlineTrackerRepoImpl(session: Session[IO]) extends OnlineTrackerRepo with OnlineTrackerCodecs {

  override def getWorld(name: String): IO[WorldRow] = {
    val q: Query[String, WorldRow] =
      sql"""
           SELECT id, name
           FROM world
           WHERE name = $varchar
      """
        .query(worldDecoder)
    session.unique(q, name)
  }

  override def getLatestSaveTime(worldId: Long): IO[OffsetDateTime] = {
    val q: Query[Long, OffsetDateTime] =
      sql"""
           SELECT MAX(time)
           FROM world_save_time
           WHERE world_id = $int8
      """
        .query(timestamptz)
    session.unique(q, worldId)
  }

  def getAllOnline(worldId: Long): IO[List[OnlineNameTime]] = {
    val q: Query[Long, OnlineNameTime] =
      sql"""
           SELECT character.name, currently_online.login_time
           FROM currently_online
           JOIN character ON currently_online.character_id = character.id
           WHERE currently_online.world_id = $int8
      """
        .query(onlineNameTimeDecoder)

    prepareToList(q, worldId)
  }

  override def getMaxSequenceId(worldId: Long): IO[Long] = {
    val q: Query[Long, Long] =
      sql"""
           SELECT MAX(sequence_id)
           FROM world_save_time
           WHERE world_id = $int8
      """
        .query(int8)
    session.unique(q, worldId)
  }

  override def insertWorldSaveTime(w: WorldSaveTimeRow): IO[Long] = {
    val q: Query[WorldSaveTimeRow, Long] =
      sql"""
           INSERT INTO world_save_time(world_id, sequence_id, time)
           VALUES $worldSaveTimeEncoder
           RETURNING id
      """
        .query(int8)
    session.unique(q, w)
  }

  override def createCharacterIfNotExists(character: CharacterRow): IO[Unit] = {
    val c: Command[CharacterRow] =
      sql"""
           INSERT INTO character(name, created)
           VALUES $characterEncoder
           ON CONFLICT DO NOTHING
      """
        .command
    session.prepare(c).flatMap(_.execute(character)).void
  }

  override def insertOnline(online: OnlineNameTime, worldId: Long): IO[Unit] = {
    val c: Command[String ~ Long ~ Long] =
      sql"""
           INSERT INTO currently_online(character_id, world_id, login_time)
           VALUES (
             (SELECT id FROM character WHERE name = $varchar),
             $int8,
             $int8)
      """
        .command
    session.prepare(c).flatMap(_.execute(online.name ~ worldId ~ online.loginTime)).void
  }

  override def deleteOnline(name: String, worldId: Long): IO[Unit] = {
    val c: Command[String ~ Long] =
      sql"""
           DELETE FROM currently_online O
           USING character C
           WHERE O.character_id = C.id
           AND C.name = $varchar
           AND O.world_id = $int8
      """
        .command
    session.prepare(c).flatMap(_.execute(name ~ worldId)).void
  }

  private def prepareToList[A, B](q: Query[A, B], args: A): IO[List[B]] =
    session.prepare(q).flatMap(_.stream(args, 64).compile.toList)

}
