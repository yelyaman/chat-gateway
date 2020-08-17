package kz.coders.chat.gateway.actors.amqp

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import kz.coders.chat.gateway.actors.dialogflow.DialogFlowActor.ProcessMessage
import kz.domain.library.messages.UserMessages
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

object AmqpListenerActor {
  def props(dialogFlowActor: ActorRef): Props = Props(new AmqpListenerActor(dialogFlowActor))
}

class AmqpListenerActor(dialogflowRef: ActorRef) extends Actor with ActorLogging {

  implicit val formats: DefaultFormats = DefaultFormats

  override def receive: Receive = {
    case msg: String =>
      val userMessage = parse(msg).extract[UserMessages]
      log.info(s"Listener received message $userMessage")
      userMessage.replyTo match {
        case Some(replyTo) =>
          dialogflowRef ! ProcessMessage(replyTo, userMessage.message.getOrElse(""), userMessage.sender)
        case None => log.info("Nothing to send as response")
      }
  }
}
