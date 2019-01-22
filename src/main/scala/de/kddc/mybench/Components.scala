package de.kddc.mybench

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

//class DatabaseDriver(connectionUri: String)

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

trait MongoDbComponentsBase {
  def mongoDbDatabaseName: String
//  implicit def mongoDb: DatabaseDriver
  implicit def mongoDb: DefaultDB
}

trait DefaultMongoDbComponents extends MongoDbComponentsBase {
  this: ServiceComponentsBase =>
  private lazy val uri = MongoConnection.parseURI(config.getString("mongodb.uri")).get
  private lazy val driver = MongoDriver(config)
  private lazy val connection = driver.connection(uri)
  lazy val mongoDbDatabaseName = config.getString("mongodb.database-name")
  implicit lazy val mongoDb = Await.result(connection.database(mongoDbDatabaseName), 10.seconds)
}