package kz.coders.chat.gateway.actors

import java.io.FileInputStream
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.{GoogleCredentials, ServiceAccountCredentials}
import com.google.cloud.dialogflow.v2.{DetectIntentRequest, QueryInput, SessionName, SessionsClient, SessionsSettings, TextInput}
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import kz.coders.chat.gateway.actors.DialogFlowActor.ProcessMessage
import kz.domain.library.messages.{Response, TelegramSender}

object DialogFlowActor {
  def props(amqpPublisher: ActorRef) = Props(new DialogFlowActor(amqpPublisher))

  case class ProcessMessage(routingKey: String, message: String, sender: TelegramSender)
}

class DialogFlowActor(amqpPublisher: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case command: ProcessMessage =>
      val response = getDialogflowResponse(command.message)
      log.info(s"I received response => $response")
      amqpPublisher ! SendResponse(command.routingKey, Response(command.sender, response))
  }

  val credentials: GoogleCredentials = GoogleCredentials.fromStream(
    new FileInputStream("C:\\Users\\User\\IdeaProjects\\citybus-bot-ebpb-264e93b44bf2.json")
  )

  val projectId: String = credentials.asInstanceOf[ServiceAccountCredentials].getProjectId

  val client: SessionsClient = SessionsClient.create(
    SessionsSettings
      .newBuilder()
      .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
      .build()
  )

  val session: SessionName = SessionName.of(projectId, UUID.randomUUID().toString)

  def getDialogflowResponse(request: String) =
    client.detectIntent(
      DetectIntentRequest
        .newBuilder()
        .setQueryInput(
          QueryInput
            .newBuilder()
            .setText(TextInput.newBuilder().setText(request).setLanguageCode("ru").build())
        ).setSession(session.toString).build()
    ).getQueryResult.getFulfillmentText
}
