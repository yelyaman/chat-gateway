package kz.coders.chat.gateway.actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.Materializer
import com.themillhousegroup.scoup.ScoupImplicits
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kz.coders.chat.gateway.utils.RestClientImpl
import kz.domain.library.messages._
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

object CityBusActor {

  def props(
      url: String
  )(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new CityBusActor(url))

}

class CityBusActor(val cityBusUrlPrefix: String)(implicit
    val system: ActorSystem,
    materializer: Materializer
) extends Actor
    with ActorLogging
    with ScoupImplicits {
  val config: Config  = ConfigFactory.load()
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
        case Failure(exception) =>
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
    case GetVehInfo(routingKey, sender, vehType, busNum) =>
      log.info("Пришла команда GetVehInfo")
      val sender = context.sender
      val id     = getIdByNum(state, busNum, vehType)
      log.info(s"Vehicle id is $busNum -> $id")
      id match {
        case -1 =>
          sender ! GetVehInfoResponse(
            "Автобуса с таким номером не существует, проверьте правильность запроса")
        case _ =>
          getBusInfo(id).onComplete {
            case Success(busses) =>
              log.info(
                s"Trying to send ${getInfoByNum(busses, state, busNum, vehType)} to sender")
              sender ! GetVehInfoResponse(
                getInfoByNum(busses, state, busNum, vehType))
            case Failure(exception) =>
              sender ! GetBusError(
                "Что то пошло не так, пожалуйста повторите позже")
          }
      }
  }

  def parseWebPage =
    Future(Jsoup.connect("https://www.citybus.kz").timeout(3000).get())

  def getVehNumbers(webPage: Document, vehType: String): List[String] =
    getBlocks(webPage, vehType)
      .select("span")
      .filter(x =>
        x.attr("style") == "vertical-align:top;margin-right:5px;float:right")
      .map(x => x.text().trim)
      .toList

  def getBlocks(webPage: Document, vehType: String): Elements =
    webPage.select(s".route-button-$vehType")

  def getBusInfo(busIndex: Int): Future[Busses] =
    RestClientImpl
      .get(
        s"https://www.citybus.kz/almaty/Monitoring/GetRouteInfo/$busIndex?_=${System.currentTimeMillis()}")
      .map(x => parse(x).extract[Busses])

  def getIdByNum(webPage: Document, num: String, veh: String): Int =
    webPage
      .select(s".route-button-$veh")
      .find(x =>
        x.getElementsByAttributeValue(
          "style",
          "vertical-align:top;margin-right:5px;float:right")
          .text() == num) match {
      case Some(value) => value.attr("id").split("-").toList.last.toInt
      case None        => -1
    }

  def getInfoByNum(
      busses: Busses,
      webPage: Document,
      busNum: String,
      busType: String
  ): String = {
    val busAmount = busses.V.length
    webPage
      .select(s".route-button-$busType")
      .find(x =>
        x.getElementsByAttributeValue(
          "style",
          "vertical-align:top;margin-right:5px;float:right")
          .text() == busNum) match {
      case Some(divBlock) =>
        val routeInfo = divBlock.select(".route-info")
        val parking   = routeInfo.select("span").remove().text
        println(parking)
        val mainInfo = routeInfo.toString
          .split("\n")
          .toList
          .map(elem => elem.replace("<br>", ""))
        s"${mainInfo(1)}\n ${parking}\n ${mainInfo(3)}\n ${mainInfo(4)}\n ${mainInfo(5)}\n   Количество транспорта $busAmount"
      case None => "NONE"
    }
  }

  def getBusAmount(busIndex: Int): Future[Int] =
    RestClientImpl
      .get(
        s"https://www.citybus.kz/almaty/Monitoring/GetRouteInfo/$busIndex?_=${System.currentTimeMillis()}")
      .map(x => parse(x).extract[Busses].V.length)

  def getAddressByCoordinates(x: Double, y: Double): Future[AddressName] =
    RestClientImpl
      .get(
        s"https://www.citybus.kz/almaty/Navigator/GetAddress/$x/$y?_=${System.currentTimeMillis()}")
      .map(x => parse(x).extract[AddressName])

}
