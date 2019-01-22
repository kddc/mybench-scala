package de.kddc.mybench.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.kddc.mybench.repositories.BenchRepository.Bench
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait HttpProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val BenchJsonFormat: RootJsonFormat[Bench] = jsonFormat3(Bench)
}
