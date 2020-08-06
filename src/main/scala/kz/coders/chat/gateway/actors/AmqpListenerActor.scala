package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, Props}
import kz.domain.library.messages.UserMessages
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

object AmqpListenerActor {
  def props() = Props(new AmqpListenerActor())
}

class AmqpListenerActor extends Actor with ActorLogging {

  implicit val formats: DefaultFormats = DefaultFormats

  override def receive: Receive = {
    case msg: String =>
      val userMessage = parse(msg).extract[UserMessages]
      log.info(s"ACTOR received message $userMessage")
  }
}
