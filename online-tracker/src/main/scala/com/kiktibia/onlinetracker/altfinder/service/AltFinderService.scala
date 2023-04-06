package com.kiktibia.onlinetracker.altfinder.service

import cats.effect.{IO, Sync}
import com.kiktibia.onlinetracker.altfinder.repo.AltFinderRepo
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class AltFinderService(repo: AltFinderRepo) {

  implicit def logger[F[_] : Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  def printOnlineTimes(characterName: String): IO[Unit] = {
    for {
      t <- repo.getOnlineTimes(characterName)
      _ <- Logger[IO].info(t.map(i => s"${i.start} - ${i.end}").mkString(", "))
    } yield IO.unit
  }

}
