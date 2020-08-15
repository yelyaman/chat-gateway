package actors

import actors.CitybusActor.{Busses, GetBusError, GetBusNum, GetBusNumResponse, GetTrollNum, GetTrollNumResponse, GetVehInfo, GetVehInfoResponse, ParseWebPage, PopulateState}
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import com.themillhousegroup.scoup.ScoupImplicits
import com.typesafe.config.Config
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import routes.Routes.Request
import utils.RestClientImpl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CitybusActor {

  def props(config: Config)(implicit system: ActorSystem, materializer: Materializer): Props = Props(new CitybusActor(config))

  trait CityBusResponse

  case class ParseWebPage()

  case class PopulateState(doc: Document)

  case class GetBusNum()
  case class GetTrollNum()

  case class GetBusNumResponse(numbers: List[String])   extends CityBusResponse
  case class GetTrollNumResponse(numbers: List[String]) extends CityBusResponse

  case class GetVehInfo(vehType: String, busNum: String) extends Request
  case class GetVehInfoResponse(busses: Busses)          extends CityBusResponse with PerRequestResponse

  case class GetBusError(error: String) extends CityBusResponse

  case class BaseInfo(
                       I: Int,
                       P: Int,
                       N: String,
                       D: Double,
                       Dab: Int,
                       Dba: Int,
                       S: Int
                     )
  case class VehicleInfo(
                          Id: Int,
                          Nm: String,
                          Tp: Int,
                          Md: String,
                          Py: Int,
                          Pc: String,
                          Cp: Int,
                          Sc: Int
                        )
  case class Busses(
                     R: BaseInfo,
                     V: Array[VehicleInfo],
                   )
}

class CitybusActor(config: Config)(implicit val system: ActorSystem, materializer: Materializer)
  extends Actor
    with ActorLogging
    with ScoupImplicits {

  val cityBusUrlPrefix: String = config.getString("application.cityBusUrlPrefix")
  var state: Document = Document.createShell(cityBusUrlPrefix)

  implicit val executionContext: ExecutionContext  = context.dispatcher
  implicit val defaultFormats: DefaultFormats.type = DefaultFormats

  override def preStart(): Unit = {
    log.info("Начинаю престарт")
    self ! ParseWebPage()
    super.preStart()
  }

  override def receive: Receive = {
    case PopulateState(doc) =>
      state = doc
      log.info(s"Стейт перезаписан -> ${state.title}")
    case ParseWebPage() =>
      log.info("Кейс ParseWebPage началось")
      parseWebPage.onComplete {
        case Success(value) =>
          log.info(s"Парсирование успешно")
          self ! PopulateState(value)
        case Failure(_) =>
          log.info("Парсирование не удалось... Начинаю заново")
          self ! ParseWebPage()
      }
    case GetBusNum =>
      log.info("Пришла команда GetBusNum")
      val sender = context.sender
      sender ! GetBusNumResponse(getVehNumbers(state, "bus"))
    case GetTrollNum =>
      val sender = context.sender
      sender ! GetTrollNumResponse(getVehNumbers(state, "troll"))
    case GetVehInfo(vehType, busNum) =>
      val sender = context.sender
      val id     = getIdByNum(state, busNum, vehType)
      id match {
        case -1 =>
          sender ! GetBusError("Автобуса с таким номером не существует, проверьте правильность запроса")
        case _ =>
          getBusInfo(id).onComplete {
            case Success(value)     => sender ! GetVehInfoResponse(value)
            case Failure(exception) => sender ! GetBusError(exception.getMessage)
          }
      }
  }

  def parseWebPage: Future[Document] = Future(Jsoup.connect(cityBusUrlPrefix).timeout(3000).get())

  def getVehNumbers(webPage: Document, vehType: String): List[String] =
    getBlocks(webPage, vehType)
      .select("span")
      .filter(x => x.attr("style") == "vertical-align:top;margin-right:5px;float:right")
      .map(x => x.text().trim)
      .toList

  def getBlocks(webPage: Document, vehType: String): Elements = webPage.select(s".route-button-$vehType")

  def getBusInfo(busIndex: Int): Future[Busses] =
    RestClientImpl
      .get(s"https://www.citybus.kz/almaty/Monitoring/GetRouteInfo/$busIndex?_=${System.currentTimeMillis()}")
      .map(x => parse(x).extract[Busses])

  def getIdByNum(webPage: Document, num: String, veh: String): Int =
    webPage
      .select(s".route-button-$veh")
      .find(x =>
        x.getElementsByAttributeValue("style", "vertical-align:top;margin-right:5px;float:right").text() == num
      ) match {
      case Some(value) => value.attr("id").split("-").toList.last.toInt
      case None        => -1
    }
}