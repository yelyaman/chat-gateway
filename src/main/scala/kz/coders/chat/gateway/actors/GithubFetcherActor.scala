package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import kz.coders.chat.gateway.utils.RestClientImpl
import kz.domain.library.messages._
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object GithubFetcherActor {
  def props(url: String)(implicit system: ActorSystem, materializer: Materializer): Props = Props(new GithubFetcherActor(url))
}

class GithubFetcherActor(val gitHubUrlPrefix: String)(implicit system: ActorSystem, materializer: Materializer) extends Actor with ActorLogging {

  implicit val executionContext: ExecutionContext = context.dispatcher
  implicit val defaultFormats: DefaultFormats.type = DefaultFormats

  override def receive: Receive = {
    case request: GetUserDetails =>
      log.debug(s"LOGGG: GetUserDetails... started, request -> $request")
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
