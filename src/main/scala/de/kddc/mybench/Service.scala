package de.kddc.mybench

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import de.kddc.mybench.repositories.BenchRepository

import scala.util.{Failure, Success}

trait ServiceComponents {
  this: ServiceComponentsBase with MongoDbComponentsBase =>
  lazy val benchRepository = new BenchRepository()
  lazy val httpServer = new HttpServer(benchRepository)
}

class Service(implicit val actorSystem: ActorSystem)
  extends ServiceComponents
    with DefaultServiceComponents
    with DefaultMongoDbComponents {

  def start() = {
    Http().bindAndHandle(httpServer.routes, "127.0.0.1", 8080).onComplete {
      case Success(binding) =>
        println(s"Successfully bound to ${binding.localAddress}")
      case Failure(error) =>
        println(s"Binding failed\n$error")
        System.exit(1)
    }
  }
}
