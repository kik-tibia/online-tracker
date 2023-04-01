package com.kiktibia.onlinetracker.repo

import com.kiktibia.onlinetracker.repo.Model.*
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

  val characterEncoder: Encoder[CharacterRow] =
    (varchar ~ timestamptz).values.contramap(
      (c: CharacterRow) => c.name ~ c.created
    )

  val onlineNameTimeDecoder: Decoder[OnlineNameTime] =
    (varchar ~ int8).map {
      case characterName ~ loginTime => OnlineNameTime(characterName, loginTime)
    }
}
