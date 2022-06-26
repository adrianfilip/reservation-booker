package com.adrianfilip.booker.scaleaware.building

import com.adrianfilip.booker.domain.model.Building
import zio.json.{DeriveJsonCodec, JsonCodec}

case class GetAllBuildingsResponse(buildings: List[Building])
object GetAllBuildingsResponse:
  implicit val codec: JsonCodec[GetAllBuildingsResponse] = DeriveJsonCodec.gen[GetAllBuildingsResponse]
