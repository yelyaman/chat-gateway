package kz.coders.telegram.actors

import kz.coders.telegram.actors.AmqpPublisherActor.SendMessage
import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import com.typesafe.config.Config
import kz.domain.library.messages.{TelegramSender, UserMessages}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel, config: Config) = Props(new AmqpPublisherActor(channel, config))
  case class SendMessage(sender: TelegramSender, message: String)
}

class AmqpPublisherActor(channel: Channel, config: Config) extends Actor with ActorLogging {

  implicit val formats: DefaultFormats = DefaultFormats

  val gatewayInExchange: String = config.getString("rabbitmq.gatewayInExchange")

  override def receive: Receive = {
    case msg: SendMessage =>
      log.info(s"actor received message ${msg.message}")
      val userMessage = UserMessages(msg.sender, Some(msg.message), Some("user.chat.telegram.response"))
      val jsonMessage = write(userMessage)
      Try(
        channel.basicPublish(
          gatewayInExchange,
          "user.chat.message",
          MessageProperties.TEXT_PLAIN,
          jsonMessage.getBytes()
        )
      ) match {
        case Success(value)     => log.info(s"Message sended to exchange ${msg.message}")
        case Failure(exception) => log.warning(s"Message doesn't send ${exception.getMessage}")
      }
  }
}
