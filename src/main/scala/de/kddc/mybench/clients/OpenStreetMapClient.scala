package de.kddc.mybench.clients

import akka.NotUsed
import akka.stream.scaladsl.{RestartSource, Source}
import com.softwaremill.sttp._
import com.softwaremill.sttp.playJson._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import de.kddc.mybench.utils.{BBox, BBoxLocation, SnakeSource}
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object OpenStreetMapClientProtocol extends PlayJsonSupport {
  final case class OpenStreetMapNode(id: Long, `type`: String, lat: Double, lon: Double)
  final case class OpenStreetMapResponse(version: Double, generator: String, elements: Seq[OpenStreetMapNode])

  implicit val OpenStreetMapNodeJsonFormat = Json.format[OpenStreetMapNode]
  implicit val OpenStreetMapResponseJsonFormat = Json.format[OpenStreetMapResponse]
}

class OpenStreetMapClient(implicit ec: ExecutionContext, sttpBackend: SttpBackend[Future, Nothing]) {
  import OpenStreetMapClientProtocol._
  import OpenStreetMapClient._

  def searchNodes(location: BBoxLocation): Future[Seq[OpenStreetMapNode]] = {
    searchNodes(BBox.fromLocation(location.latitude, location.longitude))
  }

  def searchNodes(bbox: BBox): Future[Seq[OpenStreetMapNode]] = {
    val query = s"""[out:json][timeout:25];\n(\nnode[\"amenity\"=\"bench\"](${bbox.south},${bbox.west},${bbox.north},${bbox.east});\n);\nout body;"""
    val body: Map[String, String] = Map(
      "data" -> query
    )
    request()
      .post(uri"https://overpass-api.de/api/interpreter")
      .body(body)
      .response(asJson[OpenStreetMapResponse])
      .send()
      .map(parseResponse)
      .map(_.elements)
  }

  def streamNodes(location: BBoxLocation): Source[OpenStreetMapNode, NotUsed] = {
    streamNodes(BBox.fromLocation(location.latitude, location.longitude))
  }

  def streamNodes(bbox: BBox): Source[OpenStreetMapNode, NotUsed] = {
    RestartSource.onFailuresWithBackoff(1.second, 1.minute, 0.2, 10) { () =>
      Source.fromFuture(searchNodes(bbox)).flatMapConcat(nodes => Source(nodes.toList))
    }
  }

  def streamAllNodes(center: BBoxLocation): Source[OpenStreetMapNode, NotUsed] = {
    val length = 0.05
    SnakeSource()
      .map { case (x, y) =>
        BBox(
          north = center.latitude + (0.5 + y) * length,
          south = center.latitude + (-0.5 + y) * length,
          west = center.longitude + (-0.5 + x) * length,
          east = center.longitude + (0.5 + x) * length
        )
      }
      .map { bbox =>
        println(bbox)
        bbox
      }
      .flatMapConcat(streamNodes)
      //.flatMapMerge(4, streamNodes)
  }

  private def request(): RequestT[Empty, String, Nothing] = {
    sttp
  }

}

final case class OpenStreetMapError(status: Int, message: String) extends RuntimeException {
  override def getMessage: String =
    s"OpenStreetMap request caused error [${status}], message [$message]"
}

object OpenStreetMapClient extends LazyLogging {
  private def parseResponse[T](res: Response[Either[DeserializationError[JsError], T]]): T = {
    res match {
      case Response(Right(Right(t)), _, _, _, _) =>
        t
      case Response(Right(Left(jsError)), _, _, _, _) =>
        val err = OpenStreetMapError(status = 200, jsError.message)
        logger.error(err.getMessage())
        throw err
      case Response(Left(message), code, _, _, _) =>
        val err = OpenStreetMapError(status = code, new String(message, "UTF-8"))
        logger.error(err.getMessage())
        throw err
    }
  }
}