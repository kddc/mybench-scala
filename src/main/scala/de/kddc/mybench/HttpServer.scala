package de.kddc.mybench

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import de.kddc.mybench.repositories.BenchRepository
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object HttpServerJsonProtocol extends DefaultJsonProtocol {
  implicit val BenchJsonFormat: RootJsonFormat[Bench] = jsonFormat3(Bench)
}

class HttpServer(benchRepository: BenchRepository)(implicit executionContext: ExecutionContext, materializer: ActorMaterializer) extends SprayJsonSupport {

  import HttpServerJsonProtocol._

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

  def routes = pathPrefix("benches")(concat(listBenchesRoute, listBenchesRouteStreaming, listBenchesRouteWebsocket, retrieveBenchRoute))

  def onSuccessAndDefined[T](res: Future[Option[T]]): Directive1[T] = {
    onSuccess(res).flatMap {
      case Some(value) => provide(value)
      case None => reject
    }
  }
}
