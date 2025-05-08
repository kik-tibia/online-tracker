package com.kiktibia.onlinetracker.common.repo

import cats.Monad
import cats.effect.kernel.*
import cats.syntax.all.*
import skunk.{Query, Session}

trait SkunkExtensions[F[_]] {
  val session: Session[F]

  def prepareToList[A, B](q: Query[A, B], args: A)(using Async[F]): F[List[B]] = Async[F]
    .blocking { session.prepare(q).flatMap(_.stream(args, 1048576).compile.toList) }.flatten
}
