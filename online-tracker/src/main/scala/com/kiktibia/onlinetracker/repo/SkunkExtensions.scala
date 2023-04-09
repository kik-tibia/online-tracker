package com.kiktibia.onlinetracker.repo

import cats.Monad
import cats.effect.kernel.*
import cats.syntax.all.*
import skunk.{Query, Session}

trait SkunkExtensions[F[_]] {
  implicit val FM: Monad[F]
  implicit val FC: Concurrent[F]
  val session: Session[F]

  def prepareToList[A, B](q: Query[A, B], args: A): F[List[B]] =
    session.prepare(q).flatMap(_.stream(args, 8192).compile.toList)
}
