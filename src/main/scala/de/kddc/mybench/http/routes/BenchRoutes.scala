package de.kddc.mybench.http.routes

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import com.typesafe.scalalogging.LazyLogging
import de.kddc.mybench.http.{HttpProtocol, HttpRoutes}
import de.kddc.mybench.repositories.BenchRepository
import de.kddc.mybench.repositories.BenchRepository.Bench

import scala.concurrent.ExecutionContext

class BenchRoutes(benchRepository: BenchRepository)(implicit executionContext: ExecutionContext, materializer: ActorMaterializer)
  extends HttpRoutes
  with HttpProtocol
  with LazyLogging {

  def routes = pathPrefix("benches")(
    concat(
      listBenchesRoute,
      listBenchesRouteStreaming,
      retrieveBenchRoute,
      createBenchRoute
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
}
