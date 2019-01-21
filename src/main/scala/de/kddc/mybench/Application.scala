package de.kddc.mybench

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext

case class Bench(id: Long, longitude: Double, latitude: Double)

object HttpServerJsonProtocol extends DefaultJsonProtocol {
  implicit val BenchJsonFormat: RootJsonFormat[Bench] = jsonFormat3(Bench)
}

object Application {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("mybench")
    implicit val executorExecution = ExecutionContext.Implicits.global
    implicit val materializer = ActorMaterializer()
    implicit val database = new DatabaseDriver("fake")


    val service = new Service()
    service.start()
  }
}

class DatabaseDriver(connectionUri: String)