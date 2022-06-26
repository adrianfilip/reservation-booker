package com.adrianfilip.booker.scaleaware.room

import com.adrianfilip.booker.domain.model.Room
import zio.json.{DeriveJsonCodec, JsonCodec}

case class GetRoomsResponse(rooms: List[Room])
object GetRoomsResponse:
  implicit val codec: JsonCodec[GetRoomsResponse] = DeriveJsonCodec.gen[GetRoomsResponse]
