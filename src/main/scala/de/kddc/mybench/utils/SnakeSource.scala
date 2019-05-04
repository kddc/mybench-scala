package de.kddc.mybench.utils

import akka.NotUsed
import akka.stream.scaladsl.Source

object SnakeSource {
  def apply(): Source[(Long, Long), NotUsed] = {
    Source.unfold(0)(n => Some((n + 1, coordinates(n)))).map { case (x, y, _) => (x, y) }
    //    Source.unfold(0)({
    //      case n if n < 9 => Some(n + 1, coordinates(n))
    //      case _ => None
    //    }).map {
    //      case (x, y, _) => (x, y)
    //    }
  }

  private def coordinates(n: Long): (Long, Long, Long) = {
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
