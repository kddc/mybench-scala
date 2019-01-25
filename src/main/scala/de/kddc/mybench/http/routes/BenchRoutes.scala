package de.kddc.mybench.http.routes

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.scalalogging.LazyLogging
import de.kddc.mybench.clients.OpenStreetMapClient
import de.kddc.mybench.http.{HttpProtocol, HttpRoutes}
import de.kddc.mybench.repositories.BenchRepository
import de.kddc.mybench.repositories.BenchRepository.{Bench, Location}
import de.kddc.mybench.utils.{BBoxLocation => UtilsLocation}
import de.kddc.mybench.utils.MyExtendedSource._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class BenchRoutes(benchRepository: BenchRepository, openStreetMapClient: OpenStreetMapClient)(implicit executionContext: ExecutionContext, materializer: ActorMaterializer)
  extends HttpRoutes
  with HttpProtocol
  with LazyLogging {

  def routes = pathPrefix("benches")(
    concat(
      listBenchesRoute,
      listBenchesRouteStreaming,
      retrieveBenchRoute,
      createBenchRoute,
      importBenchesChunkRoute,
      importBenchesStreamRoute,
      importBenchesStreamChunkRoute,
    ))

  def listBenchesRoute = pathEnd {
    get {
      val benchesF = benchRepository.all.toMat(Sink.collection)(Keep.right).run()// benchRepository.all.runWith(Sink.collection)
      onSuccess(benchesF) {
        case benches => complete(benches)
        //case (benches, _) => complete(benches)
      }
    }
  }

  def listBenchesRouteStreaming = path("stream") {
    get {
      val benchesS = benchRepository.all

      implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
        EntityStreamingSupport.json()
          .withParallelMarshalling(parallelism = 8, unordered = false)

      complete(benchesS)
    }
  }

  def retrieveBenchRoute = path(JavaUUID) { id =>
    get {
      onSuccess(benchRepository.findById(id)) {
        case Some(bench) => complete(bench)
        case None => reject
      }
    }
  }

  def createBenchRoute = pathEnd {
    post {
      entity(as[Bench]) { body =>
        onSuccess(benchRepository.create(body)) { bench =>
          complete(bench)
        }
      }
    }
  }

  def importBenchesChunkRoute = path("import" / "chunk") {
    get {
      parameters('lat.as[Double], 'long.as[Double]) {
        (lat, long) => {
          val nodesF = openStreetMapClient.searchNodes(UtilsLocation(latitude = lat, longitude = long))
          val benchesF = nodesF.map(_.map(node => Bench(name = node.id.toString, location = Location(node.lat, node.lon))))
          val benchesP = benchesF.flatMap(benches => Future.sequence(benches.map(bench => benchRepository.create(bench))))
          complete(benchesP)
        }
      }
    }
  }

  def importBenchesStreamRoute = path("import" / "stream") {
    get {
      parameters('lat.as[Double], 'long.as[Double]) {
        (lat, long) => {
          val countF = openStreetMapClient.streamAllNodes(UtilsLocation(latitude = lat, longitude = long))
            .map(node => Bench(name = node.id.toString, location = Location(node.lat, node.lon)))
            .map { bench =>
              println(bench)
              bench
            }
            .mapAsyncChunked(16, 1.second)(benchRepository.createMany)
            .runFold(0L)((count, _) => count + 1)
          complete(countF.map(ImportResult.apply))
        }
      }
    }
  }

  def importBenchesStreamChunkRoute = path("import" / "stream" / "chunk") {
    get {
      parameters('lat.as[Double], 'long.as[Double]) {
        (lat, long) => {
          val countF = openStreetMapClient.streamNodes(UtilsLocation(latitude = lat, longitude = long))
            .map(node => Bench(name = node.id.toString, location = Location(node.lat, node.lon)))
            .map { bench =>
              println(bench)
              bench
            }
            .mapAsync(4)(benchRepository.create)
            .runFold(0L)((count, _) => count + 1)
          //complete(countF.map(ImportResult.apply))
          onSuccess(countF) { count =>
            complete(ImportResult(count))
          }
        }
      }
    }
  }
}
