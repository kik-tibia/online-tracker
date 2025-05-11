package com.kiktibia.onlinetracker.common.repo

import cats.effect.IO
import cats.syntax.all.*
import skunk.Query
import skunk.Session

import java.util.concurrent.Executors

trait SkunkExtensions {
  val session: Session[IO]

  def prepareToList[A, B](q: Query[A, B], args: A): IO[List[B]] = session.stream(q, args, 65536).compile.toList

}
