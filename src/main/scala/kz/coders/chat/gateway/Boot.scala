package kz.coders.chat.gateway

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import kz.coders.chat.gateway.actors.AmqpListenerActor
import kz.coders.chat.gateway.amqp.{AmqpConsumer, RabbitMqConnection}

import scala.util.{Failure, Success}

object Boot extends App {

  val config: Config = ConfigFactory.load()
  val system = ActorSystem("chat-gateway")

  val ref = system.actorOf(AmqpListenerActor.props())

  val connection = RabbitMqConnection.getRabbitMqConnection(
    "guest",
    "guest",
    "127.0.0.1",
    5672,
    "/")

  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, "X:chat-gateway", "topic") match {
    case Success(_) => system.log.info("successfully declared exchange")
    case Failure(exception) => system.log.info(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(
    channel,
    "Q:gateway-queue",
    "X:chat-gateway",
    "user.chat.message"
  )

  channel.basicConsume("Q:gateway-queue", AmqpConsumer(ref))


}
