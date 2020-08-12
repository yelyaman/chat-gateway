package kz.coders.telegram

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import kz.coders.telegram.actors.{ AmqpListenerActor, AmqpPublisherActor }
import kz.coders.telegram.amqp.{ AmqpConsumer, RabbitMqConnection }
import kz.domain.library.messages.Serializers

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

object Boot extends App {
  implicit val system: ActorSystem        = ActorSystem("telegram-demo")
  implicit val materializer: Materializer = ActorMaterializer.create(system)
  implicit val ex: ExecutionContext       = system.dispatcher

  val connection = RabbitMqConnection.getRabbitMqConnection("guest", "guest", "127.0.0.1", 5672, "/")

  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, "X:chat.in.gateway", "topic") match {
    case Success(_)         => system.log.info("successfully declared exchange IN")
    case Failure(exception) => system.log.info(s"couldn't declare exchange IN ${exception.getMessage}")
  }

  RabbitMqConnection.declareExchange(channel, "X:chat.out.gateway", "topic") match {
    case Success(_)         => system.log.info("successfully declared exchange OUT")
    case Failure(exception) => system.log.info(s"couldn't declare exchange OUT ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(
    channel,
    "Q:chat.telegram.response",
    "X:chat.out.gateway",
    "user.chat.telegram.response"
  )

  val logger: LoggingAdapter = system.log
  val config                 = ConfigFactory.load()

  val cityBusUrl = config.getString("application.cityBusUrlPrefix")
  val gitHubUrl  = config.getString("application.gitHubUrlPrefix")
  val tgToken    = config.getString("telegram.token")

  val publisherActor = system.actorOf(AmqpPublisherActor.props(channel))

  val service: TelegramService = new TelegramService(tgToken, publisherActor, logger)
  val listenerActor            = system.actorOf(AmqpListenerActor.props(service))

  channel.basicConsume("Q:chat.telegram.response", AmqpConsumer(listenerActor))

  service.run()
  system.log.debug("Started Boot.scala")
}
