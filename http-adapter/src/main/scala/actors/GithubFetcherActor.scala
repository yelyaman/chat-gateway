package actors

import actors.GithubFetcherActor.{GetFailure, GetUserDetails, GetUserDetailsResponse, GetUserRepos, GetUserReposResponse, GithubRepository, GithubUser}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import routes.Routes.Request
import utils.RestClientImpl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object GithubFetcherActor {
  def props(url: String)(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new GithubFetcherActor(url))

  trait GetResponse
  case class GetUserDetails(login: String)               extends Request
  case class GetUserDetailsResponse(details: GithubUser) extends GetResponse with PerRequestResponse
  case class GetUserRepos(login: String)
  case class GetUserReposResponse(repos: List[GithubRepository]) extends GetResponse with PerRequestResponse
  case class GetFailure(error: String)                           extends GetResponse

  case class GithubUser(
                         login: String,
                         name: String,
                         location: Option[String],
                         company: Option[String]
                       )

  case class GithubRepository(
                               name: String,
                               description: Option[String],
                               full_name: String,
                               fork: Boolean,
                               language: String
                             )
}

class GithubFetcherActor(val gitHubUrlPrefix: String)(implicit system: ActorSystem, materializer: Materializer)
  extends Actor
    with ActorLogging {

  implicit val executionContext: ExecutionContext  = context.dispatcher
  implicit val defaultFormats: DefaultFormats.type = DefaultFormats

  override def receive: Receive = {


        case request: GetUserDetails =>
          log.debug(s"LOG: GetUserDetails... started, request -> $request")
          val sender = context.sender
          getGithubUser(request.login).onComplete{
            case Success(value) => sender ! GetUserDetailsResponse(value)
            case Failure(exception) => sender ! GetFailure(exception.getMessage)
          }
        case request: GetUserRepos =>
          val sender = context.sender
          getUserRepositories(request.login).onComplete{
            case Success(value) => sender ! GetUserReposResponse(value)
            case Failure(exception) => sender ! GetFailure(exception.getMessage)
          }
  }
  def getGithubUser(username: String): Future[GithubUser] =
    RestClientImpl.get(s"$gitHubUrlPrefix/$username").map(x => parse(x).extract[GithubUser])

  def getUserRepositories(username: String): Future[List[GithubRepository]] =
    RestClientImpl.get(s"$gitHubUrlPrefix/$username/repos").map(x => parse(x).extract[List[GithubRepository]])
}