package routes

import actors.CitybusActor.GetVehInfo
import actors.{CitybusMiddleWare, GithubFetcherActor}
import actors.GithubFetcherActor.GetUserDetails
import actors.PerRequest.PerRequestActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, Serialization}
import routes.Routes.Request

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._

object Routes {
  trait Request
}

class Routes(cityBusActor: ActorRef)(implicit val ex: ExecutionContext, system: ActorSystem)
  extends Json4sSupport {

  implicit val formats: DefaultFormats      = DefaultFormats
  implicit val serialization: Serialization = Serialization
  implicit val timeout: Timeout             = 5.seconds

  val config: Config     = ConfigFactory.load()
  val gitHubUrl: String  = config.getString("application.gitHubUrlPrefix")
  val cityBusUrl: String = config.getString("application.cityBusUrlPrefix")

  val handlers: Route = pathPrefix("api") {
    pathPrefix("github") {
      post {
        entity(as[GetUserDetails])(body => ctx => completeRequest(body, ctx, GithubFetcherActor.props(gitHubUrl)))
      }
    } ~ pathPrefix("citybus") {
      post {
        entity(as[GetVehInfo])(body => ctx => completeRequest(body, ctx, CitybusMiddleWare.props(cityBusActor)))
      }
    }
  }

  def completeRequest(body: Request, ctx: RequestContext, props: Props): Future[RouteResult] = {
    val promise = Promise[RouteResult]
    system.actorOf(
      Props(new PerRequestActor(body, props, promise, ctx))
    )
    promise.future
  }
}