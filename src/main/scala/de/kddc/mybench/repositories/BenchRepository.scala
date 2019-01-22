package de.kddc.mybench.repositories

import java.util.UUID

import akka.NotUsed
import akka.stream.ThrottleMode
import akka.stream.scaladsl.Source
import reactivemongo.api.DefaultDB

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

class BenchRepository(db: DefaultDB) {
  import BenchRepository._
  def all: Source[Bench, NotUsed] = {
    Source(1 to 10)
      .map { i =>
        println(i)
        i
      }
      .map(i => Bench(UUID.randomUUID, Math.random() * 90, Math.random() * 90))
      .throttle(1, 500.millis, 1, ThrottleMode.shaping)
  }

  def findById(id: UUID): Future[Option[Bench]] = {
    val random = new Random
    if (random.nextBoolean()) {
      val bench = Bench(id, Math.random() * 90, Math.random() * 90)
      Future.successful(Some(bench))
    } else Future.successful(None)
  }
}

object BenchRepository {

  final case class Bench(
    id: UUID = UUID.randomUUID,
    longitude: Double,
    latitude: Double
  )

}
