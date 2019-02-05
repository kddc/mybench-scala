package de.kddc.mybench.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import de.kddc.mybench.http.HttpProtocol
import de.kddc.mybench.{ServiceComponents, ServiceTest}

class BenchRoutesTest extends ServiceTest with ServiceComponents with HttpProtocol {
  val routes = httpServer.routes

  "should import benches" in {
    Get("/benches/import/stream/chunk?lat=53.9330084&long=9.5539501") ~> routes ~> check {
      status should be(StatusCodes.OK)
      val result = responseAs[ImportResult]
      result.count should be(2)
    }
  }
}
