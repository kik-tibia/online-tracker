package com.kiktibia.onlinetracker.tracker.tibiadata

import cats.effect.IO
import cats.effect.Sync
import cats.effect.kernel.Concurrent
import cats.effect.kernel.Resource
import cats.implicits.*
import com.kiktibia.onlinetracker.tracker.tibiadata.TibiaDataClientAlg
import com.kiktibia.onlinetracker.tracker.tibiadata.TibiaDataDecoders
import com.kiktibia.onlinetracker.tracker.tibiadata.response.CharacterResponse
import com.kiktibia.onlinetracker.tracker.tibiadata.response.WorldResponse
import io.circe.generic.auto.*
import org.http4s.Status
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.middleware.GZip
import org.http4s.client.middleware.Retry
import org.http4s.client.middleware.RetryPolicy
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.uri

import scala.concurrent.duration.*

object TibiaDataHttp4sClient {
  private val retryPolicy: RetryPolicy[IO] = (_, result, unsuccessfulAttempts) => {
    if unsuccessfulAttempts > 2 then None else if result.exists(_.status == Status.Ok) then None else 1.second.some
  }

  val clientResource: Resource[IO, Client[IO]] = EmberClientBuilder.default[IO].withTimeout(10.seconds).build
    .map(GZip()(_)).map(Retry[IO](retryPolicy)(_))
}

class TibiaDataHttp4sClient[F[_]: Sync](client: Client[F])(using Concurrent[F])
    extends TibiaDataClientAlg[F] with TibiaDataDecoders {

  private val apiRoot = uri"https://api.tibiadata.com/v5"

  def getWorld(world: String): F[WorldResponse] = {
    val target = apiRoot / "world" / world
    client.expect(target)(using jsonOf[F, WorldResponse])
  }

  def getCharacter(name: String): F[CharacterResponse] = {
    val target = apiRoot / "character" / name
    client.expect(target)(using jsonOf[F, CharacterResponse])
  }

}
