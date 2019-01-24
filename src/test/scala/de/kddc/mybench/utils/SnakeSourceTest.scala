package de.kddc.mybench.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class SnakeSourceTest extends TestKit(ActorSystem()) with WordSpecLike with BeforeAndAfterAll with Matchers with ScalaFutures {
  implicit val materializer = ActorMaterializer()

  "should emit snake coordinates" in {
    val coordinates = SnakeSource().take(6).runWith(Sink.collection).futureValue
    coordinates should be(Vector(
      (0, 0),
      (0, -1),
      (1, -1),
      (1, 0),
      (1, 1),
      (0, 1)
    ))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }
}
