package actors

import akka.actor.{ Actor, ActorLogging, Props }
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.{ RequestContext, RouteResult }
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import kz.domain.library.messages.PerRequestResponse
import kz.domain.library.messages.routes.Routes.Request
import org.json4s.{ DefaultFormats, Serialization }
import org.json4s.native.Serialization

import scala.concurrent.{ ExecutionContext, Promise }

object PerRequest {
  class PerRequestActor(
    val request: Request,
    val childProps: Props,
    val promise: Promise[RouteResult],
    val requestContext: RequestContext
  ) extends PerRequest

}

trait PerRequest extends Actor with ActorLogging with Json4sSupport {

  implicit val formats: DefaultFormats.type = DefaultFormats
  implicit val serialization: Serialization = Serialization
  implicit val ex: ExecutionContext         = context.dispatcher

  val request: Request
  val childProps: Props
  val promise: Promise[RouteResult]
  val requestContext: RequestContext

  context.actorOf(childProps) ! request

  override def receive: Receive = {
    case obj: PerRequestResponse =>
      populateResponse(obj)
      context.stop(self)
  }

  def populateResponse(obj: ToResponseMarshallable): Unit =
    requestContext
      .complete(obj)
      .onComplete(something => promise.complete(something))
}
