package de.kddc.mybench.http

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import de.kddc.mybench.repositories.BenchRepository

import scala.concurrent.{ExecutionContext}

class BenchRoutes(benchRepository: BenchRepository)(implicit executionContext: ExecutionContext, materializer: ActorMaterializer)
  extends HttpRoutes
    with HttpProtocol {

  def routes = pathPrefix("benches")(
    concat(
      listBenchesRoute,
      listBenchesRouteStreaming,
      listBenchesRouteWebsocket,
      retrieveBenchRoute
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

  // will block eventually because incoming messages are not consumed properly
  def listBenchesRouteWebsocket = path("websocket") {
    get {
      val receiving = Sink.ignore
      val benchesF = benchRepository.all
      val sending = benchesF.map { bench =>
        val json = BenchJsonFormat.write(bench)
        TextMessage(json.compactPrint)
      }
      val flow = Flow.fromSinkAndSource(receiving, sending)
      handleWebSocketMessages(flow)
    }
  }

  def retrieveBenchRoute = path(LongNumber) { id =>
    get {
      onSuccess(benchRepository.findById(id)) {
        case Some(bench) => complete(bench.toString)
        case None => reject
      }
    }
  }
}
