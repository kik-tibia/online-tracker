package com.kiktibia.onlinetracker.common.repo

import cats.Monad
import cats.effect.kernel.*
import cats.syntax.all.*
import skunk.{Query, Session}
import cats.effect.unsafe.IORuntime
import cats.effect.IO

trait SkunkExtensions {
  val session: Session[IO]

  def prepareToList[A, B](q: Query[A, B], args: A): IO[List[B]] = session.prepare(q)
    .flatMap(_.stream(args, 65536).compile.toList)

}
