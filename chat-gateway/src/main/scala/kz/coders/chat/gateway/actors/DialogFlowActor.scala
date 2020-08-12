package kz.coders.chat.gateway.actors

import java.io.FileInputStream
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.DetectIntentRequest
import com.google.cloud.dialogflow.v2.QueryInput
import com.google.cloud.dialogflow.v2.QueryResult
import com.google.cloud.dialogflow.v2.SessionName
import com.google.cloud.dialogflow.v2.SessionsClient
import com.google.cloud.dialogflow.v2.SessionsSettings
import com.google.cloud.dialogflow.v2.TextInput
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import kz.coders.chat.gateway.actors.DialogFlowActor.ProcessMessage
import kz.domain.library.messages.github.{GetUserDetails, GetUserRepos}
import kz.domain.library.messages.{GetLocationName, GetRoutes, GetVehInfo, Response, TelegramSender}

object DialogFlowActor {

  case class ProcessMessage(
    routingKey: String,
    message: String,
    sender: TelegramSender
  )

}

class DialogFlowActor(publisher: ActorRef, requesterActor: ActorRef) extends Actor with ActorLogging {

  val credentials: GoogleCredentials = GoogleCredentials.fromStream(
    new FileInputStream("""C:\Users\User\IdeaProjects\citybus-bot-ebpb-264e93b44bf2.json""")
  )

  val projectId: String =
    credentials.asInstanceOf[ServiceAccountCredentials].getProjectId

  val client: SessionsClient = SessionsClient.create(
    SessionsSettings
      .newBuilder()
      .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
      .build()
  )

  val session: SessionName =
    SessionName.of(projectId, UUID.randomUUID().toString)

  override def receive: Receive = {
    case command: ProcessMessage =>
      val response = getDialogflowResponse(command.message)
      log.info(s"Received intent name is ${response.getIntent.getDisplayName}")
      response.getIntent.getDisplayName match {
        case "get-github-account-details" =>
          val params = getValueByParameter(response, "git-account")
          log.info(s"I must give you details of $params")
          requesterActor ! GetUserDetails(command.routingKey, params, command.sender)
        case "get-github-repos-details" =>
          val params = getValueByParameter(response, "git-account")
          requesterActor ! GetUserRepos(command.routingKey, params, command.sender)

        case "get-bus-details" | "get-troll-details" =>
          val vehNum  = response.getQueryText.split(" ").last
          val vehType = response.getIntent.getDisplayName.split("-")(1)
          log.info(s"I must give you details of $vehType with busNum $vehNum")
          requesterActor ! GetVehInfo(command.routingKey, command.sender, vehType, vehNum)
        case "get-location" =>
          val coordinates = response.getQueryText.split(",")
          val x           = coordinates.head
          val y           = coordinates.last
          requesterActor ! GetLocationName(command.routingKey, command.sender, x, y)
        case "route-second-coord" =>
          val firstAddress = getValueByParameter(response, "first-coordinates")
          val secondAddress = getValueByParameter(response, "second-coordinates")
          requesterActor ! GetRoutes(command.routingKey, command.sender, firstAddress, secondAddress)
        case _ =>
          log.info(s"Last case ... => ${response.getFulfillmentText}, SENDER => ${command.sender}")
          publisher ! SendResponse(command.routingKey, Response(command.sender, response.getFulfillmentText))
      }
  }

  def getDialogflowResponse(request: String): QueryResult =
    client
      .detectIntent(
        DetectIntentRequest
          .newBuilder()
          .setQueryInput(
            QueryInput
              .newBuilder()
              .setText(
                TextInput
                  .newBuilder()
                  .setText(request)
                  .setLanguageCode("ru")
                  .build()
              )
          )
          .setSession(session.toString)
          .build()
      )
      .getQueryResult

  def getValueByParameter(response: QueryResult, param: String) =
    response.getParameters.getFieldsMap
      .get(param)
      .getStringValue

}
