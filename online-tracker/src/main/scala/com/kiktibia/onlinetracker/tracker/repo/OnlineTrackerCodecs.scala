package com.kiktibia.onlinetracker.tracker.repo

import com.kiktibia.onlinetracker.tracker.repo.Model.*
import skunk.codec.all.{int8, timestamptz, varchar}
import skunk.implicits.{sql, toIdOps}
import skunk.{Decoder, Encoder, ~}

trait OnlineTrackerCodecs {
  val worldDecoder: Decoder[WorldRow] =
    (int8 ~ varchar).map {
      case id ~ name => WorldRow(id, name)
    }

  val worldSaveTimeEncoder: Encoder[WorldSaveTimeRow] =
    (int8 ~ int8 ~ timestamptz).values.contramap(
      (r: WorldSaveTimeRow) => r.worldId ~ r.sequenceId ~ r.saveTime
    )

  val characterNameHistoryEncoder: Encoder[CharacterNameHistoryRow] =
    (int8 ~ varchar ~ timestamptz ~ timestamptz).values.contramap(
      (c: CharacterNameHistoryRow) => c.characterId ~ c.name ~ c.fromDate ~ c.toDate
    )

  val characterEncoder: Encoder[CharacterRow] =
    (varchar ~ timestamptz ~ timestamptz).values.contramap(
      (c: CharacterRow) => c.name ~ c.created ~ c.currentNameSince
    )

  val characterDecoder: Decoder[CharacterRow] =
    (int8 ~ varchar ~ timestamptz ~ timestamptz).map {
      case id ~ characterName ~ loginTime ~ currentNameSince => CharacterRow(Some(id), characterName, loginTime, currentNameSince)
    }

  val onlineNameTimeDecoder: Decoder[OnlineNameTime] =
    (varchar ~ int8).map {
      case characterName ~ loginTime => OnlineNameTime(characterName, loginTime)
    }
}
