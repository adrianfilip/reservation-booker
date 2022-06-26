package com.adrianfilip.booker.scaleaware.building

import com.adrianfilip.booker.domain.model.Building
import zio.json.{DeriveJsonCodec, JsonCodec}

case class GetFilteredBuildingsRequest(code: Option[String], name: Option[String], address: Option[String])
object GetFilteredBuildingsRequest:
  implicit val codec: JsonCodec[GetFilteredBuildingsRequest] = DeriveJsonCodec.gen[GetFilteredBuildingsRequest]

case class GetFilteredBuildingsResponse(buildings: List[Building])
object GetFilteredBuildingsResponse:
  implicit val codec: JsonCodec[GetFilteredBuildingsResponse] = DeriveJsonCodec.gen[GetFilteredBuildingsResponse]
