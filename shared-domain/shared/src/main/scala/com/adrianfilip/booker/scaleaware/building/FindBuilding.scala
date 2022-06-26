package com.adrianfilip.booker.scaleaware.building

import com.adrianfilip.booker.domain.model.Building
import zio.json.{DeriveJsonCodec, JsonCodec}

case class FindBuildingResponse(building: Option[Building])
object FindBuildingResponse:
  implicit val codec: JsonCodec[FindBuildingResponse] = DeriveJsonCodec.gen[FindBuildingResponse]
