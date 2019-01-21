package de.kddc.mybench.repositories

import akka.NotUsed
import akka.stream.ThrottleMode
import akka.stream.scaladsl.Source
import de.kddc.mybench.{Bench, DatabaseDriver}

import scala.concurrent.Future
import scala.concurrent.duration._

class BenchRepository(implicit db: DatabaseDriver) {
  def all: Source[Bench, NotUsed] = {
    Source(1 to 10)
      .map { i =>
        println(i)
        i
      }
      .map(i => Bench(i, Math.random() * 90, Math.random() * 90))
      .throttle(1, 1000.millis, 1, ThrottleMode.shaping)
  }

  def findById(id: Long): Future[Option[Bench]] = {
    if (id < 100) {
      val bench = Bench(id, Math.random() * 90, Math.random() * 90)
      Future.successful(Some(bench))
    } else Future.successful(None)
  }
}

object BenchRepository {
  final case class Bench(id: Long, longitude: Double, latitude: Double)
}
