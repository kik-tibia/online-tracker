package com.kiktibia.onlinetracker.repo

import cats.Monad
import cats.effect.kernel.*
import cats.syntax.all.*
import skunk.{Query, Session}

trait SkunkExtensions[F[_]] {
  val session: Session[F]

  def prepareToList[A, B](q: Query[A, B], args: A)(using Concurrent[F]): F[List[B]] = session.prepare(q)
    .flatMap(_.stream(args, 1048576).compile.toList)
}
