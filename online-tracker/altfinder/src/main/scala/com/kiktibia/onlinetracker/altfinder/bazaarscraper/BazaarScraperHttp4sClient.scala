package com.kiktibia.onlinetracker.altfinder.bazaarscraper

import cats.effect.IO
import cats.effect.Sync
import cats.effect.kernel.Concurrent
import cats.effect.kernel.Resource
import cats.implicits.*
import com.kiktibia.onlinetracker.altfinder.bazaarscraper.BazaarScraperClientAlg
import io.circe.generic.auto.*
import org.http4s.Status
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.middleware.GZip
import org.http4s.client.middleware.Retry
import org.http4s.client.middleware.RetryPolicy
import org.http4s.implicits.uri

import scala.concurrent.duration.*

object BazaarScraperHttp4sClient {
  private val retryPolicy: RetryPolicy[IO] = (_, result, unsuccessfulAttempts) => {
    if unsuccessfulAttempts > 2 then None else if result.exists(_.status == Status.Ok) then None else 1.second.some
  }

  val clientResource: Resource[IO, Client[IO]] = BlazeClientBuilder[IO].withRequestTimeout(5.seconds).resource
    .map(GZip()(_)).map(Retry[IO](retryPolicy)(_))
}

class BazaarScraperHttp4sClient[F[_]: Sync](client: Client[F])(using Concurrent[F]) extends BazaarScraperClientAlg[F] {
  private val apiRoot = uri"https://www.exevopan.com"

  def searchCharacter(name: String): F[String] = {
    // nicknameFilter for exevopan is a "contains" rather than exact match, so here we grab a lot of results to be safe
    // and handling pagination is too much effort
    val target = (apiRoot / "api/auctions").withQueryParams(Map(
      ("nicknameFilter", name),
      ("serverSet", "Nefera"),
      ("descending", "true"),
      ("history", "true"),
      ("pageSize", "100")
    ))
    client.expect(target)
  }

}
