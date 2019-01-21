package de.kddc.mybench

import akka.actor.ActorSystem

case class Bench(id: Long, longitude: Double, latitude: Double)

object Application {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("mybench")
    val service = new Service()
    service.start()
  }
}