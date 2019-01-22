package de.kddc.mybench.http

import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Future

trait HttpRoutes { this: HttpProtocol =>
  def routes: Route
  def onSuccessAndDefined[T](res: Future[Option[T]]): Directive1[T] = {
    onSuccess(res).flatMap {
      case Some(value) => provide(value)
      case None => reject
    }
  }
}
