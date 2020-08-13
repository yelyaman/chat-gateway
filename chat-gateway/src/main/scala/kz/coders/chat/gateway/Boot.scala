package kz.coders.chat.gateway

import akka.actor.ActorSystem
import akka.actor.Props
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
  implicit val materializer: Materializer = Materializer(system)
  val config: Config                      = ConfigFactory.load()

  val username    = config.getString("rabbitmq.username")
  val password    = config.getString("rabbitmq.password")
  val host        = config.getString("rabbitmq.host")
  val port        = config.getInt("rabbitmq.port")
  val virtualHost = config.getString("rabbitmq.virtualHost")

  val gatewayInExchange = config.getString("rabbitmq.gatewayInExchange")
  val gatewayOutExchange = config.getString("rabbitmq.gatewayOutExchange")
  val gatewayQueue = config.getString("rabbitmq.gatewayQueue")

  val connection =
    RabbitMqConnection.getRabbitMqConnection(username, password, host, port, virtualHost)

  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, gatewayInExchange, "topic") match {
    case Success(_) => system.log.info("successfully declared 'X:chat.in.gateway' exchange")
    case Failure(exception) =>
      system.log.error(s"couldn't declare 'X:chat.in.gateway' exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareExchange(channel, gatewayOutExchange, "topic") match {
    case Success(_) => system.log.info("successfully declared 'X:chat.out.gateway' exchange")
    case Failure(exception) =>
      system.log.error(s"couldn't declare 'X:chat.out.gateway' exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(channel, gatewayQueue, gatewayInExchange, "user.chat.message")

  val publisher = system.actorOf(AmqpPublisherActor.props(channel, config))
  val requester = system.actorOf(RequesterActor.props(publisher, config))
  val dialogflowRef = system.actorOf(DialogFlowActor.props(publisher, requester, config))
  val listener      = system.actorOf(AmqpListenerActor.props(dialogflowRef))

  channel.basicConsume(gatewayQueue, AmqpConsumer(listener))

}
