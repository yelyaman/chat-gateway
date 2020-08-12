package kz.domain.library.messages.citybus

import kz.domain.library.messages.{PerRequestResponse, Sender}
import org.jsoup.nodes.Document

object CitybusDomain {
  trait CityBusResponse

  case class CitybusCaseClasses()

  case class ParseWebPage()

  case class PopulateState(doc: Document)

  case class GetBusNum()
  case class GetTrollNum()

  case class BusNumResponse(nums: List[String])   extends CityBusResponse
  case class TrollNumResponse(nums: List[String]) extends CityBusResponse

  case class GetVehInfo(
    routingKey: String,
    sender: Sender,
    vehType: String,
    busNum: String
  )

  case class VehInfoResponse(busses: String) extends CityBusResponse with PerRequestResponse

  case class GetLocationName(
    routingKey: String,
    sender: Sender,
    x: String,
    y: String
  )

  case class LocationNameResponse(
    locationName: String
  ) extends CityBusResponse

  case class GetRoutes(
    routingKey: String,
    sender: Sender,
    firstAddress: String,
    secondAddress: String
  )

  case class RoutesResponse(
    routes: String
  ) extends CityBusResponse

  case class GetBusError(error: String) extends CityBusResponse

  /**
   *
   * @param I
   * @param P
   * @param N
   * @param D
   * @param Dab
   * @param Dba
   * @param S
   */
  case class BaseInfo(
    I: Int,
    P: Int,
    N: String,
    D: Double,
    Dab: Int,
    Dba: Int,
    S: Int
  )

  /**
   *
   * @param Id
   * @param Nm
   * @param Tp
   * @param Md
   * @param Py
   * @param Pc
   * @param Cp
   * @param Sc
   */
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

  case class StationInfo(
    Id: Int,
    Nm: String
  )

  case class Station(
    Ss: Array[StationInfo]
  )

  case class Busses(
    R: BaseInfo,
    V: Array[VehicleInfo],
    Sc: Array[Station]
  )

  case class TransportChange(
    Id: Int,
    Sa: Int,
    Sb: Int,
    Nm: String,
    Tp: Int
  )

  case class Routes(
    Sa: Int,
    Sb: Int,
    R1: Array[TransportChange],
    R2: Array[TransportChange],
    R3: Array[TransportChange],
    R4: Array[TransportChange],
    R5: Array[TransportChange]
  )

  case class AddressName(
    Nm: String,
    Pt: List[Double]
  )

  case class CoordByAddressName(
    Nm: String,
    X: Double,
    Y: Double
  )
}
