package kz.domain.library.messages

import org.jsoup.nodes.Document

case class CitybusCaseClasses()

case class ParseWebPage()
case class PopulateState(doc: Document)

trait CityBusResponse
case class GetBusNum()
case class GetTrollNum()

case class GetBusNumResponse(nums: List[String])   extends CityBusResponse
case class GetTrollNumResponse(nums: List[String]) extends CityBusResponse

case class GetVehInfo(
    routingKey: String,
    sender: Sender,
    vehType: String,
    busNum: String
)

case class GetVehInfoResponse(busses: String)
    extends CityBusResponse
    with PerRequestResponse

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
    V: Array[VehicleInfo]
)

case class AddressName(
    Nm: String,
    Pt: List[Double]
)
