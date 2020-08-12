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
import kz.domain.library.messages.citybus.CitybusDomain.{
  AddressName,
  BusNumResponse,
  Busses,
  GetBusError,
  GetBusNum,
  GetLocationName,
  GetRoutes,
  GetTrollNum,
  GetVehInfo,
  LocationNameResponse,
  ParseWebPage,
  PopulateState,
  Routes,
  StationInfo,
  TransportChange,
  TrollNumResponse,
  VehInfoResponse
}
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

class CityBusActor(val cityBusUrlPrefix: String)(
  implicit
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
      sender ! BusNumResponse(getVehNumbers(state, "bus"))
    case GetTrollNum =>
      val sender = context.sender
      sender ! TrollNumResponse(getVehNumbers(state, "troll"))
    case GetVehInfo(routingKey, sender, vehType, busNum) =>
      log.info("Пришла команда GetVehInfo")
      val sender = context.sender
      val id     = getIdByNum(state, busNum, vehType)
      log.info(s"Vehicle id is $busNum -> $id")
      id match {
        case -1 =>
          sender ! VehInfoResponse("Автобуса с таким номером не существует, проверьте правильность запроса")
        case _ =>
          getBusInfo(id).onComplete {
            case Success(busses) =>
              val response = getInfoByNum(busses, state, busNum, vehType)
              sender ! VehInfoResponse(response)
            case Failure(exception) =>
              sender ! GetBusError("Что то пошло не так, пожалуйста повторите позже")
          }
      }
    case GetLocationName(routingKey, sender, x, y) =>
      log.info(s"Пришла команда GetLocationName(${x}, $y)")
      val sender = context.sender
      getAddressByCoordinates(x, y).onComplete {
        case Success(value) =>
          sender ! LocationNameResponse(value.Nm)
        case Failure(_) => sender ! GetBusError("Что то пошло не так, пожалуйста повторите")
      }

    case GetRoutes(routingKey, sender, firstAddress, secondAddress) =>
      log.info(s"Пришел запрос маршрута $firstAddress ------>>>>> $secondAddress")
      val firstCoord  = firstAddress.split("a").mkString("/")
      val secondCoord = secondAddress.split("a").mkString("/")
//      getTransplants(firstCoord, secondCoord).onComplete{
//        case Success(value) => println(value)
//        case Failure(exception) => println(exception.getMessage)
//      }
      getRoutes(firstCoord, secondCoord).onComplete {
        case Success(value)     => log.info(s"Value ================= ${value.toList}")
        case Failure(exception) => log.info(s"Value ================= ${exception.getMessage}")
      }
  }

  def parseWebPage: Future[Document] =
    Future(Jsoup.connect("https://www.citybus.kz").timeout(3000).get())

  def getVehNumbers(webPage: Document, vehType: String): List[String] =
    getBlocks(webPage, vehType)
      .select("span")
      .filter(x => x.attr("style") == "vertical-align:top;margin-right:5px;float:right")
      .map(x => x.text().trim)
      .toList

  def getBlocks(webPage: Document, vehType: String): Elements =
    webPage.select(s".route-button-$vehType")

  def getBusInfo(busIndex: Int): Future[Busses] =
    RestClientImpl
      .get(s"https://www.citybus.kz/almaty/Monitoring/GetRouteInfo/$busIndex?_=${System.currentTimeMillis()}")
      .map(x => parse(x).extract[Busses])

  def getIdByNum(webPage: Document, num: String, veh: String): Int =
    webPage
      .select(s".route-button-$veh")
      .find(x =>
        x.getElementsByAttributeValue("style", "vertical-align:top;margin-right:5px;float:right")
          .text() == num
      ) match {
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
        x.getElementsByAttributeValue("style", "vertical-align:top;margin-right:5px;float:right")
          .text() == busNum
      ) match {
      case Some(divBlock) =>
        val routeInfo = divBlock.select(".route-info")
        val parking   = routeInfo.select("span").remove().text
        val mainInfo = routeInfo.toString
          .split("\n")
          .toList
          .map(elem => elem.replace("<br>", ""))
        s"${mainInfo(1)}\n   $parking\n ${mainInfo(3)}\n ${mainInfo(4)}\n ${mainInfo(5)}\n   Количество транспорта $busAmount"
      case None => "NONE"
    }
  }

  def getAddressByCoordinates(x: String, y: String): Future[AddressName] = {
    log.info(s"X: $x, Y: $y")
    RestClientImpl
      .get(s"https://www.citybus.kz/almaty/Navigator/GetAddress/$x/$y?_=${System.currentTimeMillis()}")
      .map(x => parse(x).extract[AddressName])
  }

  def getRoutes(firstCoord: String, secondCoord: String): Future[List[Routes]] =
    RestClientImpl
      .get(
        s"https://www.citybus.kz/almaty/Navigator/FindRoutes/$firstCoord/$secondCoord?_=${System.currentTimeMillis()}"
      )
      .map(x => parse(x).extract[Array[Routes]].toList.map(x => x))

  def getBusAmount(busIndex: Int): Future[Int] =
    RestClientImpl
      .get(s"https://www.citybus.kz/almaty/Monitoring/GetRouteInfo/$busIndex?_=${System.currentTimeMillis()}")
      .map(x => parse(x).extract[Busses].V.length)

  def getTransplants(firstCoord: String, secondCoord: String): Future[Unit] =
    getRoutes(firstCoord, secondCoord).map { routes =>
      val routeAmount = routes.length
      routes.foreach { route =>
        val startPoint = route.Sa
        val endPoint   = route.Sb
        val transplants = {
          collectTransplants(route.R1.toList, route.R2.toList, route.R3.toList, route.R4.toList, route.R5.toList)
        }

        log.info(s"TRANSPLANTs => $transplants")
      }
    }

  def collectTransplants(
    t1: List[TransportChange],
    t2: List[TransportChange],
    t3: List[TransportChange],
    t4: List[TransportChange],
    t5: List[TransportChange]
  ): List[TransportChange] =
    List(t1, t2, t3, t4, t5).filter(change => change.nonEmpty).flatten

  def transplantsResult(result: String, transplants: List[TransportChange]) = transplants match {
    case Nil => result
    case x :: xs =>
      val startPoint   = x.Sa
      val endPoint     = x.Sb
      val transportNum = x.Nm
      val vehttype = x.Tp match {
        case 0 => "bus"
        case 1 => "troll"
      }
      val vehType = x.Tp match {
        case 0 => "Автобус"
        case 1 => "Троллейбус"
      }
      getPlacesByIds(startPoint, endPoint, transportNum, vehttype).onComplete{
        case Success(value) =>
//        Я нашел всю нужную информацию, теперь нужно только объеденить их в стринг и возвратить фунцию
//          transplantsResult(re)
      }
  }

  def getPlacesByIds(startPoint: Int, endPoint: Int, transportNum: String, vehttype: String) = {
    val id = getIdByNum(state, transportNum, vehttype)
      getBusInfo(id).map(busses => busses.Sc.Ss.filter(station => station.Id == startPoint).toList.head).flatMap{ startLoc =>
        getBusInfo(id).map(busses => busses.Sc.Ss.filter(station => station.Id == endPoint).toList.head).map(endLoc =>
        List(startLoc.Nm, endLoc.Nm))
      }
  }
}
