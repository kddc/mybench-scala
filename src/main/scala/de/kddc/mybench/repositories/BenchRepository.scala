package de.kddc.mybench.repositories

import java.util.UUID

import akka.{Done, NotUsed}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import de.kddc.mybench.utils.BaseBSONProtocol
import reactivemongo.api.{Cursor, DefaultDB}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentHandler, Macros}
import reactivemongo.akkastream._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Random, Success}

class BenchRepository(db: DefaultDB)(implicit ex: ExecutionContext, mat: ActorMaterializer) {
  import BenchRepository.BSONProtocol._
  import BenchRepository._

  private val collection = Await.result(init(db), 10.seconds)

  def all: Source[Bench, Future[State]] = {
    collection
      .find(BSONDocument.empty)
      .cursor[Bench]()
      .documentSource()
      //.throttle(1, 50.millis, 1, ThrottleMode.shaping)
      //.mapMaterializedValue(_ => NotUsed)
  }

  def all(from: Int = 0, limit: Int = 10): Future[(Seq[Bench], Long)] = {
    val selector = BSONDocument.empty
    for {
      elems <- collection
        .find(selector)
        .skip(from)
        .cursor[Bench]()
        .collect[Seq](limit, Cursor.FailOnError[Seq[Bench]]())
      count <- collection.count(Some(selector))
    } yield (elems, count)
  }

  def findById(id: UUID): Future[Option[Bench]] = {
    collection.find(BSONDocument("_id" -> id)).one[Bench]
  }

  def findByName(name: String): Future[Option[Bench]] = {
    collection.find(BSONDocument("name" -> name)).one[Bench]
  }

  def create(bench: Bench): Future[Bench] = {
    collection.insert(bench).map(_ => bench)
  }

  def createMany(benches: Seq[Bench]): Future[Seq[Bench]] = {
    collection.insert[Bench](false).many(benches).map(_ => benches)
  }
}

object BenchRepository extends LazyLogging {
  final case class Bench(id: UUID = UUID.randomUUID, name: String, location: Location)
  final case class Location(longitude: Double, latitude: Double)

  private def init(db: DefaultDB)(implicit ec: ExecutionContext): Future[BSONCollection] = {
    val collectionName = "benches"
    val collection = db.collection[BSONCollection](collectionName)
    for {
      collections <- db.collectionNames
      _ <- if (!collections.contains(collection.name)) for {
        _ <- collection.create()
        _ <- fill(collection)
      } yield Done
      else Future.successful(Done)
    } yield collection
  }

  private val seed = (1 to 10).map(i => Bench(name = s"Bench #$i", location = Location(Math.random * 180 - 90, Math.random * 180 - 90)))

  private def fill(collection: BSONCollection)(implicit ec: ExecutionContext): Future[Done] = {
    import BSONProtocol._
    Future.sequence(seed.map { bench =>
      logger.info(s"Adding [$bench] to database")
      collection.insert(bench)
    }).map(_ => Done)
  }

  object BSONProtocol extends BaseBSONProtocol {
    implicit val Benchhandler: BSONDocumentHandler[Bench] = Macros.handler[Bench]
    implicit val Locationhandler: BSONDocumentHandler[Location] = Macros.handler[Location]
  }
}
