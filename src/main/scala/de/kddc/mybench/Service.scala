package de.kddc.mybench

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import de.kddc.mybench.repositories.BenchRepository

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class Service(implicit actorSystem: ActorSystem, executor: ExecutionContext, materializer: ActorMaterializer, db: DatabaseDriver) {
  val benchRepository = new BenchRepository()
  val httpServer = new HttpServer(benchRepository)

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
