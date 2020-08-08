package kz.coders.chat.gateway

import java.io.FileInputStream

import akka.actor.ActorSystem
import com.google.auth.oauth2.{GoogleCredentials, ServiceAccountCredentials}
import com.typesafe.config.{Config, ConfigFactory}
import kz.coders.chat.gateway.actors.{AmqpListenerActor, AmqpPublisherActor, DialogFlowActor}
import kz.coders.chat.gateway.amqp.{AmqpConsumer, RabbitMqConnection}

import scala.util.{Failure, Success}

object Boot extends App {

  val config: Config = ConfigFactory.load()
  val system = ActorSystem("chat-gateway")

  val connection = RabbitMqConnection.getRabbitMqConnection(
    "guest",
    "guest",
    "127.0.0.1",
    5672,
    "/")

  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, "X:chat.in.gateway", "topic") match {
    case Success(_) => system.log.info("successfully declared IN exchange")
    case Failure(exception) => system.log.info(s"couldn't declare IN exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareExchange(channel, "X:chat.out.gateway", "topic") match {
    case Success(_) => system.log.info("successfully declared OUT exchange")
    case Failure(exception) => system.log.info(s"couldn't declare OUT exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(
    channel,
    "Q:gateway-queue",
    "X:chat.in.gateway",
    "user.chat.message"
  )
  val publisher = system.actorOf(AmqpPublisherActor.props(channel))
  val dialogflowRef = system.actorOf(DialogFlowActor.props(publisher))
  val listener = system.actorOf(AmqpListenerActor.props(dialogflowRef))

  channel.basicConsume("Q:gateway-queue", AmqpConsumer(listener))

}
