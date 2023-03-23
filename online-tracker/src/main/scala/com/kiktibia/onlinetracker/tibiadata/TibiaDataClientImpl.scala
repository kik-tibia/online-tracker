package com.kiktibia.onlinetracker.tibiadata

import cats.effect.IO
import cats.effect.kernel.Resource
import com.kiktibia.onlinetracker.tibiadata.response.*
import io.circe.generic.auto.*
import io.circe.Decoder
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.client.middleware.GZip
import org.http4s.implicits.*

object TibiaDataClient {
  val clientResource: Resource[IO, Client[IO]] = BlazeClientBuilder[IO].resource.map(GZip()(_))
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
