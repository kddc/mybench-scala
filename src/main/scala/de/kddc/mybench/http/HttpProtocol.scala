package de.kddc.mybench.http

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import de.kddc.mybench.repositories.BenchRepository.{ Location, Bench }
import play.api.libs.json.Json

trait HttpProtocol extends PlayJsonSupport {
  final case class ImportResult(count: Long)

  implicit val LocationJsonFormat = Json.using[Json.WithDefaultValues].format[Location]
  implicit val BenchJsonFormat = Json.using[Json.WithDefaultValues].format[Bench]
  implicit val ImportResultJsonFormat = Json.format[ImportResult]
}
