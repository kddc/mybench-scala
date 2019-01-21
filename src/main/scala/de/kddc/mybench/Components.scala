package de.kddc.mybench

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

class DatabaseDriver(connectionUri: String)

trait ServiceComponentsBase {
  implicit def actorSystem: ActorSystem
  implicit def executionContext: ExecutionContext
  implicit def materializer: ActorMaterializer
  //  def config: Config
}

trait DefaultServiceComponents extends ServiceComponentsBase {
  implicit lazy val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()(actorSystem)
  //  override lazy val config: Config = actorSystem.settings.config
}

trait MongoDbComponentsBase {
  def mongoDbDatabaseName: String
  implicit def mongoDb: DatabaseDriver
}

trait DefaultMongoDbComponents extends MongoDbComponentsBase {
  this: ServiceComponentsBase =>
  lazy val mongoDbDatabaseName = "mybench"
  implicit lazy val mongoDb = new DatabaseDriver(mongoDbDatabaseName)
}