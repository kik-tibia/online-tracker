package com.kiktibia.onlinetracker.tracker.tibiadata

import com.kiktibia.onlinetracker.tracker.tibiadata.response.{AccountInformation, Guild}
import io.circe.generic.auto.*
import io.circe.{Decoder, HCursor}

trait TibiaDataDecoders {
  implicit val decodeAccountInformation: Decoder[Option[AccountInformation]] = decodeEmptyObjToOption[AccountInformation]
  implicit val decodeGuild: Decoder[Option[Guild]] = decodeEmptyObjToOption[Guild]

  // required because TibiaData returns an empty object instead of null for some fields
  private def decodeEmptyObjToOption[T](implicit d: Decoder[T]): Decoder[Option[T]] = (c: HCursor) => {
    if c.value.asObject.forall(_.isEmpty) then Right(None)
    else c.as[T].map(Some(_))
  }
}
