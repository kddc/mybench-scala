package de.kddc.mybench

import de.kddc.mybench.utils.BoundingBox

object Application {
  def main(args: Array[String]): Unit = {
    val service = new Service
    service.start()
//    println("Hello Scala")
//    println(spiral(0))
    //(0 to 8).foreach(n => println(spiral(n)))
//    val bbox1 = BBox(53.91798871241739,9.499955177307129,53.94972163975539,9.56033706665039)
//    val bbox2 = BBox.fromLocation(bbox1.center.latitude, bbox1.center.longitude)
//
//    val bbox3 = BBox(53.806733341651714,9.288597106933594,54.06059694131036,9.771652221679688)
//
//
//    println(bbox1, bbox1.center)
//    println(bbox2, bbox2.center)
//    println("-------------------------------")
//    println(bbox3)
//    println("-------------------------------")
//    bbox3.subdivide(3).foreach(println)
//    System.exit(1)
  }
}