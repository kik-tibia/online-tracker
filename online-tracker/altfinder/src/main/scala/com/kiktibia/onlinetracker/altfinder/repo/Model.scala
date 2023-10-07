package com.kiktibia.onlinetracker.altfinder.repo

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneId}

object Model {
  case class OnlineSegment(characterId: Long, start: Long, end: Long)

  case class OnlineDateSegment(characterName: String, start: OffsetDateTime, end: OffsetDateTime) {
    override def toString: String = s"${characterName.padTo(32, ' ')} ${start.atLocal} - ${end.atLocal}"

    def onlineDiscordFormat = s"${start.toDiscord} - ${end.toDiscord}"

    extension (d: OffsetDateTime)

      private def atLocal: String =
        // d.atZoneSameInstant(ZoneId.of("Brazil/East")).toLocalDateTime .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        d.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

      private def toDiscord: String = {
        val unixTimestamp = d.toEpochSecond()
        s"<t:$unixTimestamp:d> <t:$unixTimestamp:t>"
      }
  }
}
