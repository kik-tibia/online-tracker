package com.kiktibia.onlinetracker.altfinder.bazaarscraper

trait BazaarScraperClientAlg[F[_]] {
  def searchCharacter(name: String): F[String]
}
