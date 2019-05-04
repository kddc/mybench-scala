package de.kddc.mybench.http

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import de.kddc.mybench.repositories.BenchRepository.{Bench, Location}
import de.kddc.mybench.repositories.UserRepository.User
import play.api.libs.json.{Json, Writes}

trait HttpProtocol extends PlayJsonSupport {
  case class ImportResult(count: Long)

  implicit val UserJsonReads = Json.using[Json.WithDefaultValues].reads[User]
  implicit val UserJsonWrites: Writes[User] = Writes { user => Json.obj(
    "id" -> user._id,
    "username" -> user.username,
  )}

  implicit val LocationJsonFormat = Json.using[Json.WithDefaultValues].format[Location]
  implicit val BenchJsonFormat = Json.using[Json.WithDefaultValues].format[Bench]
  implicit val ImportResultJsonFormat = Json.format[ImportResult]
}
