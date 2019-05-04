package de.kddc.mybench.http

import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import de.kddc.mybench.http.routes.{ AuthRoutes, BenchRoutes, UserRoutes }

import scala.concurrent.ExecutionContext

class HttpServer(
  authRoutes: AuthRoutes,
  userRoutes: UserRoutes,
  benchRoutes: BenchRoutes)(implicit executionContext: ExecutionContext, materializer: ActorMaterializer) {

  def routes = concat(
    authRoutes.routes,
    userRoutes.routes,
    benchRoutes.routes)
}
