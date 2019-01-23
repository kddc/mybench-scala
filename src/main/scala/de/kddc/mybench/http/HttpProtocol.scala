package de.kddc.mybench.http

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import de.kddc.mybench.repositories.BenchRepository.{ Location, Bench }
import play.api.libs.json.Json

trait HttpProtocol extends PlayJsonSupport {
  implicit val LocationJsonFormat = Json.using[Json.WithDefaultValues].format[Location]
  implicit val BenchJsonFormat = Json.using[Json.WithDefaultValues].format[Bench]
}
