package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import kz.domain.library.messages.{GetVehInfo, VehInfoResponse}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object CitybusMiddleWare {
  def props(cityBusActor: ActorRef): Props = Props(new CitybusMiddleWare(cityBusActor))
}

class CitybusMiddleWare(cityBusActor: ActorRef) extends Actor with ActorLogging {

  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case obj: GetVehInfo =>
      log.info(s"CitybusMiddleWare received $obj")
      val sender = context.sender()

      (cityBusActor ? obj).mapTo[VehInfoResponse].map { resp =>
        log.info(s"Received response -> $resp")
        sender ! resp
      }
  }
}
