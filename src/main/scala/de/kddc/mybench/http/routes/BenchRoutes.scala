package de.kddc.mybench.http.routes

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import de.kddc.mybench.clients.OpenStreetMapClient
import de.kddc.mybench.http.{HttpProtocol, HttpRoutes}
import de.kddc.mybench.repositories.BenchRepository
import de.kddc.mybench.repositories.BenchRepository.{Bench, Location}
import de.kddc.mybench.utils.BBox

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Success

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
      importBenchesRoute
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

  def importBenchesRoute = path("import") {
    get {
      parameters('lat.as[Double], 'long.as[Double]) {
        (lat, long) => {
//          val nodes = openStreetMapClient.findNodes(BBox.fromLocation(lat, long))
//          val benches = nodes
//            .map(nodes => nodes.map(node => Bench(name = node.id.toString, location = Location(node.lat, node.lon))))
//          val persistedBenches = benches
//              .flatMap(benches => Future.sequence(benches.map(bench => benchRepository.create(bench))))

          val countF = openStreetMapClient.streamNodes(BBox.fromLocation(lat, long))
              .map(node => Bench(name = node.id.toString, location = Location(node.lat, node.lon)))
              .groupedWithin(16, 1.second)
              .mapAsync(1)(benchRepository.createMany)
              .flatMapConcat(chunks => Source(chunks.toList))
              .runFold(0L)((count, _) => count + 1)

          onSuccess(countF) { count =>
            complete(s"Imported $count benches")
          }

//          val benchesF = openStreetMapClient.findNodes(BBox.fromLocation(lat, long))
//            .map(nodes => nodes.map(node => Bench(name = node.id.toString, location = Location(node.lat, node.lon))))
//            .map(benches => benches.map(bench => benchRepository.create(bench)))

//          onSuccess(benchesF) { benches =>
//            complete(benches)
//          }
//          benchesF.foreach(_.foreach(println))
          complete(StatusCodes.OK)
        }
      }
    }
  }
}
