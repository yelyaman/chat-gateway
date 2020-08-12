package kz.coders.chat.gateway.actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import kz.domain.library.messages.Response
import kz.domain.library.messages.Serializers
import org.json4s.jackson.Serialization.write

object AmqpPublisherActor {
  def props(channel: Channel): Props = Props(new AmqpPublisherActor(channel))

  case class SendResponse(routingKey: String, response: Response)
}

class AmqpPublisherActor(channel: Channel) extends Actor with ActorLogging with Serializers {

  override def receive: Receive = {
    case resp: SendResponse =>
      log.info(s"Publisher received resp => ${resp.response}")
      val response     = resp.response
      val jsonResponse = write(response)
      channel.basicPublish(
        "X:chat.out.gateway",
        resp.routingKey,
        MessageProperties.TEXT_PLAIN,
        jsonResponse.getBytes()
      )
  }

}
