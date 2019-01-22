package de.kddc.mybench

case class Bench(id: Long, longitude: Double, latitude: Double)

object Application {
  def main(args: Array[String]): Unit = {
    val service = new Service
    service.start()
  }
}