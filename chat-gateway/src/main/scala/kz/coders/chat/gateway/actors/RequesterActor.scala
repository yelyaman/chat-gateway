package kz.coders.chat.gateway.actors

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem }
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.Config
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import kz.domain.library.messages.Response
import kz.domain.library.messages.citybus.CitybusDomain._
import kz.domain.library.messages.github._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class RequesterActor(publisherActor: ActorRef, config: Config)(
  implicit
  val system: ActorSystem,
  materializer: Materializer
) extends Actor
    with ActorLogging {
  implicit val ex: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout     = 5.seconds

  val cityBusUrl: String = config.getString("application.cityBusUrlPrefix")
  val gitHubUrl: String  = config.getString("application.gitHubUrlPrefix")

  val githubActor: ActorRef = {
    system.actorOf(GithubFetcherActor.props(gitHubUrl))
  }
  val citybusActor: ActorRef = {
    system.actorOf(CityBusActor.props(cityBusUrl))
  }

  override def receive: Receive = {
    case request: GetUserDetails =>
      log.info("Requester received GetUserDetails")
      (githubActor ? request)
        .mapTo[GetResponse]
        .map {
          case obj: GetUserDetailsResponse =>
            publisherActor ! SendResponse(request.routingKey, Response(request.sender, obj.details))
          case err: GetFailure =>
            publisherActor ! SendResponse(request.routingKey, Response(request.sender, err.error))
        }
    case request: GetUserRepos =>
      log.info("Requester received GetUserRepos")
      (githubActor ? request)
        .mapTo[GetResponse]
        .map {
          case obj: GetUserReposResponse =>
            publisherActor ! SendResponse(request.routingKey, Response(request.sender, obj.repos))
          case err: GetFailure => err.error
        }
    case request: GetVehInfo =>
      log.info("Requester received GetVehInfo")
      (citybusActor ? request)
        .mapTo[CityBusResponse]
        .map {
          case obj: VehInfoResponse =>
            publisherActor ! SendResponse(request.routingKey, Response(request.sender, obj.busses))
        }
    case request: GetLocationName =>
      log.info(s"Requester received GetLocationName(${request.x}, ${request.y})")
      (citybusActor ? request)
        .mapTo[CityBusResponse]
        .map {
          case obj: LocationNameResponse =>
            publisherActor ! SendResponse(request.routingKey, Response(request.sender, obj.locationName))
        }
    case request: GetRoutes =>
      (citybusActor ? request)
        .mapTo[CityBusResponse]
        .map {
          case obj: RoutesResponse =>
            publisherActor ! SendResponse(request.routingKey, Response(request.sender, obj.routes))
        }
    case obj => log.warning(s"request unhandled ${obj.getClass.getName}")
  }

}
