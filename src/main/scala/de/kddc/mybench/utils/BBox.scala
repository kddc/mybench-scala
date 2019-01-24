package de.kddc.mybench.utils

case class BBox(south: Double, west: Double, north: Double, east: Double) {
  case class Location(latitude: Double, longitude: Double)

  val center: Location = {
    Location((south + north) / 2, (west + east) / 2)
  }

  def subdivide(n: Int): Seq[BBox] = {
    if (n % 2 == 0)
      throw new IllegalArgumentException("must be an odd number")
    else {
      val subX = (east - west) / n
      val subY = (north - south) / n
      (0 until (n * n))
        .map(i => cell(i))
        .map(pos => {
          Location(center.latitude + subY * pos._2, center.longitude + subX * pos._1)
        })
        .map(c => {
          BBox(c.latitude - subY / 2, c.longitude - subX / 2, c.latitude + subY / 2, c.longitude + subX / 2)
        })
    }
  }

  def cell(n: Int): (Int, Int, Int) = {
    if (n == 0)
      (0, 0, 1)
    else {
      val r = Math.max(1, (Math.floor((Math.sqrt(n) - 1) / 2) + 1).toInt)
      val p = (8 * r * (r - 1)) / 2
      val a = (n - p) % (r * 8)
      Math.floor(a / (r * 2)) match {
        case 0 => (a - r, -r, r)
        case 1 => (r, (a % (r * 2)) - r, r)
        case 2 => (r - (a % (r * 2)), r, r)
        case 3 => (-r, r - (a % (r * 2)), r)
        case _ => (0, 0, 0)
      }
    }
  }
}

object BBox {
  def fromLocation(latitude: Double, longitude: Double, distance: Int = 1000) = {
    val r: Double = distance * 0.0089982311916 / 1000
    BBox(latitude - r, longitude - r, latitude + r, longitude + r)
  }
}