package de.kddc.mybench.http

// import java.util.UUID

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
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
      // listBenchesRouteWebsocket,
    )
  )

  def listBenchesRoute = pathEnd {
    get {
      val benchesF = benchRepository.all.runWith(Sink.collection)

      onSuccess(benchesF) { benches =>
        complete(benches)
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
        println(body)
        logger.info("bench", body)
//        onSuccess(benchRepository.create(body.copy(_id = UUID.randomUUID))) { bench =>
//          complete(bench)
//        }
        complete(body)
      }
    }
  }

  // will block eventually because incoming messages are not consumed properly
  //  def listBenchesRouteWebsocket = path("websocket") {
  //    get {
  //      val receiving = Sink.ignore
  //      val benchesF = benchRepository.all
  //      val sending = benchesF.map { bench =>
  //        val json = BenchJsonFormat.writes(bench)
  //        TextMessage(Json.prettyPrint(json))
  //      }
  //      val flow = Flow.fromSinkAndSource(receiving, sending)
  //      handleWebSocketMessages(flow)
  //    }
  //  }
}
