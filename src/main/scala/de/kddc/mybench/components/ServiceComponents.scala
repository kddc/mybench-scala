package de.kddc.mybench.components

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

trait ServiceComponentsBase {
  implicit def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext
  implicit def materializer: ActorMaterializer
  def config: Config
}

trait DefaultServiceComponents extends ServiceComponentsBase {
  implicit lazy val actorSystem = ActorSystem("mybench")
  implicit lazy val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
  lazy val config: Config = actorSystem.settings.config
}
