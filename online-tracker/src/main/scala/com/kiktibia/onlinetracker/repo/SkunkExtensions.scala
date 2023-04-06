package com.kiktibia.onlinetracker.repo

import skunk.{Query, Session}
import cats.effect.IO

trait SkunkExtensions {
  val session: Session[IO]

  def prepareToList[A, B](q: Query[A, B], args: A): IO[List[B]] =
    session.prepare(q).flatMap(_.stream(args, 64).compile.toList)
}
