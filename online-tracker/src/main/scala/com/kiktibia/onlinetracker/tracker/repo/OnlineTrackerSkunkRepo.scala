package com.kiktibia.onlinetracker.tracker.repo

import cats.Monad
import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import com.kiktibia.onlinetracker.repo.SkunkExtensions
import com.kiktibia.onlinetracker.tracker.repo.Model.*
import skunk.*
import skunk.codec.all.{int8, timestamptz, varchar}
import skunk.implicits.{sql, toIdOps}

import java.time.OffsetDateTime

class OnlineTrackerSkunkRepo[F[_] : Monad](val session: Session[F])(using Concurrent[F])
  extends OnlineTrackerRepoAlg[F] with OnlineTrackerCodecs with SkunkExtensions[F] {

  override def getWorld(name: String): F[WorldRow] = {
    val q: Query[String, WorldRow] =
      sql"""
        SELECT id, name
        FROM world
        WHERE name = $varchar
      """.query(worldDecoder)
    session.unique(q, name)
  }

  override def getLatestSaveTime(worldId: Long): F[Option[OffsetDateTime]] = {
    val q: Query[Long, Option[OffsetDateTime]] =
      sql"""
        SELECT MAX(time)
        FROM world_save_time
        WHERE world_id = $int8
      """.query(timestamptz.opt)
    session.unique(q, worldId)
  }

  def getAllOnline(worldId: Long): F[List[OnlineNameTime]] = {
    val q: Query[Long, OnlineNameTime] =
      sql"""
        SELECT character.name, currently_online.login_time
        FROM currently_online
        JOIN character ON currently_online.character_id = character.id
        WHERE currently_online.world_id = $int8
      """.query(onlineNameTimeDecoder)
    prepareToList(q, worldId)
  }

  override def getMaxSequenceId(worldId: Long): F[Option[Long]] = {
    val q: Query[Long, Option[Long]] =
      sql"""
        SELECT MAX(sequence_id)
        FROM world_save_time
        WHERE world_id = $int8
      """.query(int8.opt)
    session.unique(q, worldId)
  }

  override def insertWorldSaveTime(w: WorldSaveTimeRow): F[Long] = {
    val q: Query[WorldSaveTimeRow, Long] =
      sql"""
        INSERT INTO world_save_time(world_id, sequence_id, time)
        VALUES $worldSaveTimeEncoder
        RETURNING id
      """.query(int8)
    session.unique(q, w)
  }

  override def insertCharacterNameHistory(characterNameHistory: CharacterNameHistoryRow): F[Unit] = {
    val c: Command[CharacterNameHistoryRow] =
      sql"""
        INSERT INTO character_name_history(character_id, name, from_date, until_date)
        VALUES $characterNameHistoryEncoder
      """.command
    session.prepare(c).flatMap(_.execute(characterNameHistory)).void
  }

  override def updateCharacterName(id: Long, newName: String, time: OffsetDateTime): F[Unit] = {
    val c: Command[String ~ OffsetDateTime ~ Long] =
      sql"""
        UPDATE character SET
          name = $varchar,
          current_name_since = $timestamptz
        WHERE id = $int8
      """.command
    session.prepare(c).flatMap(_.execute(newName ~ time ~ id)).void
  }

  override def insertCharacter(character: CharacterRow): F[Unit] = {
    val c: Command[CharacterRow] =
      sql"""
        INSERT INTO character(name, created, current_name_since)
        VALUES $characterEncoder
        ON CONFLICT DO NOTHING
      """.command
    session.prepare(c).flatMap(_.execute(character)).void
  }

  override def getCharacter(name: String): F[Option[CharacterRow]] = {
    val q: Query[String, CharacterRow] =
      sql"""
        SELECT id, name, created, current_name_since FROM character
        WHERE name = $varchar
      """.query(characterDecoder)
    session.option(q, name)
  }

  override def insertOnline(online: OnlineNameTime, worldId: Long): F[Unit] = {
    val c: Command[String ~ Long ~ Long] =
      sql"""
        INSERT INTO currently_online(character_id, world_id, login_time)
        VALUES (
          (SELECT id FROM character WHERE name = $varchar),
          $int8,
          $int8)
      """.command
    session.prepare(c).flatMap(_.execute(online.name ~ worldId ~ online.loginTime)).void
  }

  override def deleteOnline(name: String, worldId: Long): F[Unit] = {
    val c: Command[String ~ Long] =
      sql"""
        DELETE FROM currently_online O
        USING character C
        WHERE O.character_id = C.id
        AND C.name = $varchar
        AND O.world_id = $int8
      """.command
    session.prepare(c).flatMap(_.execute(name ~ worldId)).void
  }

  override def insertOnlineHistory(name: String, loginTime: Long, logoutTime: Long): F[Unit] = {
    val c: Command[String ~ Long ~ Long] =
      sql"""
        INSERT INTO online_history(character_id, login_time, logout_time)
        VALUES (
          (SELECT id FROM character WHERE name = $varchar),
          $int8,
          $int8)
      """.command
    session.prepare(c).flatMap(_.execute(name ~ loginTime ~ logoutTime)).void
  }

}
