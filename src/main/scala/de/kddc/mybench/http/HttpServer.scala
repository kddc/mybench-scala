package de.kddc.mybench.http

import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

class HttpServer(benchRoutes: BenchRoutes)(implicit executionContext: ExecutionContext, materializer: ActorMaterializer) {

  def routes = concat(
    benchRoutes.routes
  )
}
