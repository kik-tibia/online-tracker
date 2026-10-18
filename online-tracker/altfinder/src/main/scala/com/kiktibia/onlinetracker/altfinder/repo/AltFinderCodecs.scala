package com.kiktibia.onlinetracker.altfinder.repo

import com.kiktibia.onlinetracker.altfinder.repo.Model.*
import skunk.Decoder
import skunk.codec.all.int8
import skunk.codec.all.timestamptz
import skunk.codec.all.varchar

trait AltFinderCodecs {
  val onlineSegmentDecoder: Decoder[OnlineSegment] =
    (int8 *: int8 *: int8).map {
      case (id, start, end) => OnlineSegment(id, start, end)
    }

  val onlineDateSegmentDecoder: Decoder[OnlineDateSegment] =
    (varchar *: timestamptz *: timestamptz).map {
      case (name, start, end) => OnlineDateSegment(name, start, end)
    }
}
