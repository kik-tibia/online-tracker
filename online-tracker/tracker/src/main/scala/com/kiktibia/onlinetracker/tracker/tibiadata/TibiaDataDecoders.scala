package com.kiktibia.onlinetracker.tracker.tibiadata

import com.kiktibia.onlinetracker.tracker.tibiadata.response.{AccountInformation, Guild}
import io.circe.generic.auto.*
import io.circe.{Decoder, HCursor}

trait TibiaDataDecoders {
  given decodeAccountInformation: Decoder[Option[AccountInformation]] = decodeEmptyObjToOption[AccountInformation]
  given decodeGuild: Decoder[Option[Guild]] = decodeEmptyObjToOption[Guild]

  // required because TibiaData returns an empty object instead of null for some fields
  private def decodeEmptyObjToOption[T](using Decoder[T]): Decoder[Option[T]] = (c: HCursor) => {
    if c.value.asObject.forall(_.isEmpty) then Right(None)
    else c.as[T].map(Some(_))
  }
}
