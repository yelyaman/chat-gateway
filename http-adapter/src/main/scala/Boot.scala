import actors.{ CityBusActor, GithubFetcherActor }
import akka.actor.{ ActorSystem, Props }
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import routes.Routes

import scala.concurrent.ExecutionContext

object Boot extends App {
  implicit val system: ActorSystem        = ActorSystem("telegram-demo")
  implicit val materializer: Materializer = ActorMaterializer.create(system)
  implicit val ex: ExecutionContext       = system.dispatcher

  val logger: LoggingAdapter = system.log
  val config                 = ConfigFactory.load()

  val cityBusUrl = config.getString("application.cityBusUrlPrefix")
  val gitHubUrl  = config.getString("application.gitHubUrlPrefix")

  val gitHubActor  = system.actorOf(Props(new GithubFetcherActor(gitHubUrl)))
  val cityBusActor = system.actorOf(Props(new CityBusActor(config)))
  val host         = config.getString("application.host")
  val port         = config.getInt("application.port")

  val githubRoutes = new Routes(gitHubActor, cityBusActor)

  system.log.debug("Started Boot.scala")

  Http().bindAndHandle(githubRoutes.handlers, host, port)
}
