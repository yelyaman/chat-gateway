package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import kz.coders.chat.gateway.actors.DialogFlowActor.ProcessMessage
import kz.domain.library.messages.{Response, UserMessages}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object AmqpListenerActor {
  def props(dialogFlowActor: ActorRef) = Props(new AmqpListenerActor(dialogFlowActor))
}

class AmqpListenerActor(dialogflowRef: ActorRef) extends Actor with ActorLogging {

  implicit val formats: DefaultFormats = DefaultFormats

  override def receive: Receive = {
    case msg: String =>
      val userMessage = parse(msg).extract[UserMessages]
      log.info(s"Listener received message $userMessage")
      userMessage.replyTo match {
        case Some(replyTo) =>
          dialogflowRef ! ProcessMessage(
            replyTo,
            userMessage.message.getOrElse(""),
            userMessage.sender)
        case None => log.info("Nothing to send as response")
      }
  }
}
