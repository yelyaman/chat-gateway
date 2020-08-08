package kz.coders.chat.gateway.actors

import akka.actor.{Actor, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import kz.domain.library.messages.Response
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

object AmqpPublisherActor {
  def props(channel: Channel) = Props(new AmqpPublisherActor(channel))

  case class SendResponse(routingKey: String, response: Response)
}

class AmqpPublisherActor(channel: Channel) extends Actor {

  implicit val formats: DefaultFormats.type = DefaultFormats

  override def receive: Receive = {
    case resp: SendResponse =>
      val response = resp.response
      val jsonResponse = write(response)
      channel.basicPublish(
        "X:chat.out.gateway",
        resp.routingKey,
        MessageProperties.TEXT_PLAIN,
        jsonResponse.getBytes()
      )
  }

}
