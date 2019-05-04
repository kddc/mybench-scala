package de.kddc.mybench

import akka.http.scaladsl.Http
import com.typesafe.scalalogging.LazyLogging
import de.kddc.mybench.clients.OpenStreetMapClient
import de.kddc.mybench.components._
import de.kddc.mybench.http.HttpServer
import de.kddc.mybench.http.routes.{ AuthRoutes, BenchRoutes, UserRoutes }
import de.kddc.mybench.providers.AuthProvider
import de.kddc.mybench.repositories.{ BenchRepository, UserRepository }

import scala.util.{ Failure, Success }

trait ServiceComponents {
  this: ServiceComponentsBase with MongoDbComponentsBase with HttpClientComponentsBase =>
  lazy val userRepository = new UserRepository(mongoDb)
  lazy val benchRepository = new BenchRepository(mongoDb)
  lazy val openStreetMapClient = new OpenStreetMapClient()

  implicit lazy val authProvider = new AuthProvider(userRepository)
  lazy val authRoutes = new AuthRoutes()
  lazy val userRoutes = new UserRoutes(userRepository)
  lazy val benchRoutes = new BenchRoutes(benchRepository, openStreetMapClient)

  lazy val httpServer = new HttpServer(
    authRoutes = authRoutes,
    userRoutes = userRoutes,
    benchRoutes = benchRoutes)
}

class Service
  extends ServiceComponents
  with DefaultServiceComponents
  with DefaultMongoDbComponents
  with DefaultHttpClientComponents
  with LazyLogging {

  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  def start() = {
    Http().bindAndHandle(httpServer.routes, interface, port).onComplete {
      case Success(binding) =>
        logger.info(s"Successfully bound to ${binding.localAddress}")
      case Failure(error) =>
        logger.error("Binding failed", error)
        System.exit(1)
    }
  }
}
