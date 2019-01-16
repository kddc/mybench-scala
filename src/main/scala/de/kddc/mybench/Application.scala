package de.kddc.mybench

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{ EntityStreamingSupport, JsonEntityStreamingSupport }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server.Directive1
import akka.stream.{ ActorMaterializer, ThrottleMode }
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

case class Bench(id: Long, longitude: Double, latitude: Double)

object HttpServerJsonProtocol extends DefaultJsonProtocol {
  implicit val BenchJsonFormat: RootJsonFormat[Bench] = jsonFormat3(Bench)
}

class HttpServer(benchRepository: BenchRepository)(implicit executionContext: ExecutionContext, materializer: ActorMaterializer) extends SprayJsonSupport {
  import HttpServerJsonProtocol._

  def listBenchesRoute = pathEnd {
    get {
      //      val benchesF = benchRepository.all.toMat(Sink.collection)(Keep.right).run()
      val benchesF = benchRepository.all.runWith(Sink.collection)

      //      onSuccess(benchesF) { benches =>
      //        complete(benches.map(_.toString).mkString("\n"))
      //      }

      //      implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
      //        EntityStreamingSupport.json()
      //          .withParallelMarshalling(parallelism = 8, unordered = false)

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

    //    get {
    //      val benchesF = benchRepository.all
    //      val flow = Flow[Message].zip(benchesF).map {
    //        case (_, bench) =>
    //          val json = BenchJsonFormat.write(bench)
    //          TextMessage(json.compactPrint)
    //      }
    //      handleWebSocketMessages(flow)
    //    }
  }

  def retrieveBenchRoute = path(LongNumber) { id =>
        get {
          onSuccess(benchRepository.findById(id)) {
            case Some(bench) => complete(bench.toString)
            case None => reject
          }
        }

    //    get {
    //      onSuccess(benchRepository.findById(id)) { optionalBench =>
    //        rejectEmptyResponse {
    //          complete(optionalBench.map(_.toString))
    //        }
    //      }
    //    }

    //    get {
    //      rejectEmptyResponse {
    //        complete(benchRepository.findById(id).map(_.toString))
    //      }
    //    }
    //
    //    get {
    //      onSuccessAndDefined(benchRepository.findById(id)) { bench =>
    //        complete(bench)
    //      }
    //    }
  }

  def routes = pathPrefix("benches")(concat(listBenchesRoute, listBenchesRouteStreaming, listBenchesRouteWebsocket, retrieveBenchRoute))

  def onSuccessAndDefined[T](res: Future[Option[T]]): Directive1[T] = {
    onSuccess(res).flatMap {
      case Some(value) => provide(value)
      case None => reject
    }
  }
}

object Application {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("mybench")
    implicit val executorExecution = ExecutionContext.Implicits.global
    implicit val materializer = ActorMaterializer()
    implicit val database = new DatabaseDriver("fake")
    val benchRepository = new BenchRepository()
    val httpServer = new HttpServer(benchRepository)

    Http().bindAndHandle(httpServer.routes, "127.0.0.1", 8080).onComplete {
      case Success(binding) =>
        println(s"Successfully bound to ${binding.localAddress}")
      case Failure(error) =>
        println(s"Binding failed\n$error")
        System.exit(1)
    }
  }
}

class DatabaseDriver(connectionUri: String)

class BenchRepository(implicit db: DatabaseDriver) {
  def all: Source[Bench, NotUsed] = {
    // Source(1 to 10000000)
    Source(1 to 10)
      .map { i =>
        println(i)
        i
      }
      .map(i => Bench(i, Math.random() * 90, Math.random() * 90))
      .throttle(1, 1000.millis, 1, ThrottleMode.shaping)
  }

  def findById(id: Long): Future[Option[Bench]] = {
    if (id < 100) {
      val bench = Bench(id, Math.random() * 90, Math.random() * 90)
      Future.successful(Some(bench))
    } else Future.successful(None)
  }
}

class testClass {
  def test(id: Long) = {
    implicit val database = new DatabaseDriver("fake")
    implicit val executor = ExecutionContext.Implicits.global
    val benchRepository = new BenchRepository()
    benchRepository.findById(1).map { possibleABench =>
      possibleABench match {
        case Some(possibleABench) => possibleABench
        case None => new Bench(0,0,0)
      }
    }
    benchRepository.findById(1).map(possibleABench => {
      possibleABench match {
        case Some(possibleABench) => possibleABench
        case None => None
      }
    })
  }
}
