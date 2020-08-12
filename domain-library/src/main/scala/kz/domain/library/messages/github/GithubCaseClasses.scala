package kz.domain.library.messages.github

import kz.domain.library.messages.{PerRequestResponse, Sender}

class GithubCaseClasses()

trait GetResponse
case class GetUserDetails(routingKey: String, login: String, sender: Sender)

case class GetUserDetailsResponse(details: String)
    extends GetResponse
    with PerRequestResponse

case class GetUserRepos(routingKey: String, login: String, sender: Sender)

case class GetUserReposResponse(repos: String)
    extends GetResponse
    with PerRequestResponse

case class GetFailure(error: String) extends GetResponse

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
