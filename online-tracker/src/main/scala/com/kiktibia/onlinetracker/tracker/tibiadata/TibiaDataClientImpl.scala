package com.kiktibia.onlinetracker.tracker.tibiadata

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.syntax.all.*
import com.kiktibia.onlinetracker.tracker.tibiadata.response.*
import io.circe.generic.auto.*
import io.circe.Decoder
import org.http4s.Status
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.middleware.{GZip, Retry, RetryPolicy}
import org.http4s.implicits.*

import scala.concurrent.duration.*

object TibiaDataClient {
  private val retryPolicy: RetryPolicy[IO] = (_, result, unsuccessfulAttempts) => {
    if (unsuccessfulAttempts > 2) None
    else if (result.exists(_.status == Status.Ok)) None
    else 1.second.some
  }

  val clientResource: Resource[IO, Client[IO]] = BlazeClientBuilder[IO].withRequestTimeout(10.seconds).resource
    .map(GZip()(_))
    .map(Retry[IO](retryPolicy)(_))
}

trait TibiaDataClient {
  def getWorld(world: String): IO[WorldResponse]

  def getCharacter(name: String): IO[CharacterResponse]
}

class TibiaDataClientImpl(client: Client[IO]) extends TibiaDataClient with TibiaDataDecoders {

  private val apiRoot = uri"https://api.tibiadata.com/v3"

  def getWorld(world: String): IO[WorldResponse] = {
    val target = apiRoot / "world" / world
    client.expect(target)(jsonOf[IO, WorldResponse])
  }

  def getCharacter(name: String): IO[CharacterResponse] = {
    val target = apiRoot / "character" / name
    client.expect(target)(jsonOf[IO, CharacterResponse])
  }

}