package com.kiktibia.onlinetracker.common.repo

import cats.effect.IO
import cats.effect.Resource
import com.kiktibia.onlinetracker.common.config.DatabaseConfig
import org.typelevel.otel4s.metrics.Meter
import org.typelevel.otel4s.trace.Tracer
import skunk.Session

object SessionResource {

  private given Meter[IO] = Meter.noop
  private given Tracer[IO] = Tracer.noop

  def single(cfg: DatabaseConfig): Resource[IO, Session[IO]] = Session.Builder[IO].withHost(cfg.host).withPort(cfg.port)
    .withUserAndPassword(cfg.user, cfg.password).withDatabase(cfg.database).single

}
