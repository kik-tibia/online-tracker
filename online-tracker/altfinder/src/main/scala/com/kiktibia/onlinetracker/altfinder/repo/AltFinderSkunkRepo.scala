package com.kiktibia.onlinetracker.altfinder.repo

import cats.Monad
import cats.effect.IO
import cats.effect.kernel.Async
import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import com.kiktibia.onlinetracker.altfinder.repo.Model.*
import com.kiktibia.onlinetracker.common.repo.SkunkExtensions
import skunk.*
import skunk.codec.all.int8
import skunk.codec.all.timestamptz
import skunk.codec.all.varchar
import skunk.implicits.sql
import skunk.implicits.toIdOps

import java.time.OffsetDateTime

class AltFinderSkunkRepo(val session: Session[IO])
    extends AltFinderRepoAlg[IO] with AltFinderCodecs with SkunkExtensions {

  override def getOnlineTimes(
      characterNames: List[String],
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime]
  ): IO[List[OnlineSegment]] = {
    val cl = characterNames.map(_.toLowerCase)

    val baseFragment = sql"""
        SELECT o.character_id, o.login_time, o.logout_time
        FROM online_history o JOIN character c
        ON o.character_id = c.id
      """
    val joinFragment = sql"JOIN world_save_time w ON o.login_time = w.id"
    val charFragment = sql"WHERE LOWER(c.name) IN (${varchar.values.list(characterNames.length)})"
    val fromToFragment = sql"AND w.time >= $timestamptz AND w.time <= $timestamptz"
    val fromFragment = sql"AND w.time >= $timestamptz"
    val toFragment = sql"AND w.time <= $timestamptz"

    (from, to) match {
      case (Some(f), Some(t)) =>
        val q = sql"$baseFragment $joinFragment $charFragment $fromToFragment".query(onlineSegmentDecoder)
        prepareToList(q, (cl, (f, t)))
      case (Some(f), None) =>
        val q = sql"$baseFragment $joinFragment $charFragment $fromFragment".query(onlineSegmentDecoder)
        prepareToList(q, cl ~ f)
      case (None, Some(t)) =>
        val q = sql"$baseFragment $joinFragment $charFragment $toFragment".query(onlineSegmentDecoder)
        prepareToList(q, cl ~ t)
      case (None, None) =>
        val q = sql"$baseFragment $charFragment".query(onlineSegmentDecoder)
        prepareToList(q, cl)
    }
  }

  override def getPossibleMatches(
      characterNames: List[String],
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime],
      distance: Option[Int]
  ): IO[List[OnlineSegment]] = {
    val cl = characterNames.map(_.toLowerCase)

    val baseFragment = sql"""
        SELECT o.character_id, o.login_time, o.logout_time
        FROM online_history o
      """
    val joinFragment = sql"JOIN world_save_time w ON o.login_time = w.id"
    val whereInFragment = sql"WHERE o.character_id IN"
    val adjacencyFragment = distance match {
      case Some(d) => sql"""
          ((o1.login_time - o2.logout_time >= 0 AND o1.login_time - o2.logout_time <= #${d.toString})
           OR (o2.login_time - o1.logout_time >= 0 AND o2.login_time - o1.logout_time <= #${d.toString}))
        """
      case None => sql"""
        (o1.login_time = o2.logout_time OR o1.logout_time = o2.login_time)
      """
    }
    val innerFragment = sql"""
        SELECT DISTINCT o2.character_id
        FROM online_history o1
        JOIN online_history o2 ON $adjacencyFragment
        JOIN character c ON o1.character_id = c.id
      """
    val innerJoinFragment = sql"JOIN world_save_time w ON o2.login_time = w.id"
    val charFragment = sql"WHERE LOWER(c.name) IN (${varchar.values.list(characterNames.length)})"
    val fromToFragment = sql"AND w.time >= $timestamptz AND w.time <= $timestamptz"
    val fromFragment = sql"AND w.time >= $timestamptz"
    val toFragment = sql"AND w.time <= $timestamptz"

    (from, to) match {
      case (Some(f), Some(t)) =>
        val q =
          sql"$baseFragment $joinFragment $whereInFragment ($innerFragment $innerJoinFragment $charFragment $fromToFragment) $fromToFragment"
            .query(onlineSegmentDecoder)
        prepareToList(q, (cl, (f, t), (f, t)))
      case (Some(f), None) =>
        val q =
          sql"$baseFragment $joinFragment $whereInFragment ($innerFragment $innerJoinFragment $charFragment $fromFragment) $fromFragment"
            .query(onlineSegmentDecoder)
        prepareToList(q, (cl, f, f))
      case (None, Some(t)) =>
        val q =
          sql"$baseFragment $joinFragment $whereInFragment ($innerFragment $innerJoinFragment $charFragment $toFragment) $toFragment"
            .query(onlineSegmentDecoder)
        prepareToList(q, (cl, t, t))
      case (None, None) =>
        val q = sql"$baseFragment $whereInFragment ($innerFragment $charFragment)".query(onlineSegmentDecoder)
        prepareToList(q, cl)
    }
  }

  override def getCharacterName(characterId: Long): IO[String] = {
    val q: Query[Long, String] = sql"""
        SELECT name FROM character
        WHERE id = $int8
      """.query(varchar)
    session.unique(q, characterId)
  }

  override def getCharacterHistories(
      characterNames: List[String],
      from: Option[OffsetDateTime],
      to: Option[OffsetDateTime]
  ): IO[List[OnlineDateSegment]] = {
    val cl = characterNames.map(_.toLowerCase)

    val baseFragment = sql"""
        SELECT c.name, w1.time, w2.time
        FROM online_history o
        JOIN character c ON o.character_id = c.id
        JOIN world_save_time w1 ON o.login_time = w1.id
        JOIN world_save_time w2 ON o.logout_time = w2.id
        WHERE lower(c.name) IN (${varchar.values.list(characterNames.length)})
      """
    val fromToFragment = sql"AND w2.time >= $timestamptz AND w1.time <= $timestamptz"
    val fromFragment = sql"AND w2.time >= $timestamptz"
    val toFragment = sql"AND w1.time <= $timestamptz"
    val orderFragment = sql"ORDER BY w1.sequence_id"

    (from, to) match {
      case (Some(f), Some(t)) =>
        val q = sql"$baseFragment $fromToFragment $orderFragment".query(onlineDateSegmentDecoder)
        prepareToList(q, (cl, (f, t)))
      case (Some(f), None) =>
        val q = sql"$baseFragment $fromFragment $orderFragment".query(onlineDateSegmentDecoder)
        prepareToList(q, cl ~ f)
      case (None, Some(t)) =>
        val q = sql"$baseFragment $toFragment $orderFragment".query(onlineDateSegmentDecoder)
        prepareToList(q, cl ~ t)
      case (None, None) =>
        val q = sql"$baseFragment $orderFragment".query(onlineDateSegmentDecoder)
        prepareToList(q, cl)
    }
  }

  override def getPastCharacterNames(characterName: String): IO[List[String]] = {
    val q = sql"""
      SELECT cnh.name FROM character c JOIN character_name_history cnh ON c.id = cnh.character_id
      WHERE lower(c.name) = $varchar
    """.query(varchar)
    prepareToList(q, characterName.toLowerCase)
  }

}
