package kz.coders.chat.gateway.actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.Materializer
import kz.coders.chat.gateway.utils.RestClientImpl
import kz.domain.library.messages.github.{
  GetFailure,
  GetUserDetails,
  GetUserDetailsResponse,
  GetUserRepos,
  GetUserReposResponse,
  GithubRepository,
  GithubUser
}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

object GithubFetcherActor {

  def props(url: String)(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new GithubFetcherActor(url))

}

class GithubFetcherActor(val gitHubUrlPrefix: String)(
  implicit
  system: ActorSystem,
  materializer: Materializer
) extends Actor
    with ActorLogging {

  implicit val executionContext: ExecutionContext  = context.dispatcher
  implicit val defaultFormats: DefaultFormats.type = DefaultFormats

  override def receive: Receive = {
    case request: GetUserDetails =>
      log.debug(s"LOGGG: GetUserDetails... started, request -> $request")
      val sender = context.sender
      getGithubUser(request.login).onComplete {
        case Success(value)     => sender ! GetUserDetailsResponse(userDetailsResult(value))
        case Failure(exception) => sender ! GetFailure(exception.getMessage)
      }
    case request: GetUserRepos =>
      log.debug(s"LOGGG: GetUserRepos... started, request -> $request")
      val sender = context.sender
      getUserRepositories(request.login).onComplete {
        case Success(value)     => sender ! GetUserReposResponse(userReposResult("", value))
        case Failure(exception) => sender ! GetFailure(exception.getMessage)
      }
  }

  def getGithubUser(username: String): Future[GithubUser] =
    RestClientImpl
      .get(s"$gitHubUrlPrefix/$username")
      .map(x => parse(x).extract[GithubUser])

  def getUserRepositories(username: String): Future[List[GithubRepository]] =
    RestClientImpl
      .get(s"$gitHubUrlPrefix/$username/repos")
      .map(x => parse(x).extract[List[GithubRepository]])

  def userDetailsResult(obj: GithubUser): String =
    s"Полное имя: ${obj.name}\n" +
      s"Имя пользователя: ${obj.login}\n" +
      s"Местоположение: ${obj.location.getOrElse("Отсутствует")}\n" +
      s"Организация: ${obj.company.getOrElse("Отсутствует")}"

  def userReposResult(result: String, obj: List[GithubRepository]): String =
    obj match {
      case Nil => result
      case x :: xs =>
        userReposResult(result + s"Название: ${x.name}\n     Язык: ${x.language}\n     Описание: ${x.description
          .getOrElse("Отсутствует")}\n", xs)
    }

}
