package kz.coders.chat.gateway

import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.ActorMaterializer
import akka.stream.Materializer
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kz.coders.chat.gateway.actors.AmqpListenerActor
import kz.coders.chat.gateway.actors.AmqpPublisherActor
import kz.coders.chat.gateway.actors.DialogFlowActor
import kz.coders.chat.gateway.actors.RequesterActor
import kz.coders.chat.gateway.amqp.AmqpConsumer
import kz.coders.chat.gateway.amqp.RabbitMqConnection

import scala.util.Failure
import scala.util.Success

object Boot extends App {

  implicit val system: ActorSystem        = ActorSystem("chat-gateway")
  implicit val materializer: Materializer = ActorMaterializer.create(system)
  val config: Config                      = ConfigFactory.load()

  val connection =
    RabbitMqConnection.getRabbitMqConnection("guest", "guest", "127.0.0.1", 5672, "/")

  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, "X:chat.in.gateway", "topic") match {
    case Success(_) => system.log.info("successfully declared IN exchange")
    case Failure(exception) =>
      system.log.info(s"couldn't declare IN exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareExchange(channel, "X:chat.out.gateway", "topic") match {
    case Success(_) => system.log.info("successfully declared OUT exchange")
    case Failure(exception) =>
      system.log.info(s"couldn't declare OUT exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(channel, "Q:gateway-queue", "X:chat.in.gateway", "user.chat.message")

  val publisher = system.actorOf(AmqpPublisherActor.props(channel))
  val requester = system.actorOf(Props(new RequesterActor(publisher, config)))
  requester ! "привет"
  val dialogflowRef = system.actorOf(Props(new DialogFlowActor(publisher, requester)))
  val listener      = system.actorOf(AmqpListenerActor.props(dialogflowRef))

  channel.basicConsume("Q:gateway-queue", AmqpConsumer(listener))

}
