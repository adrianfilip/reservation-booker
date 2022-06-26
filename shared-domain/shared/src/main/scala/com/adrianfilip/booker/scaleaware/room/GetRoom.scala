package com.adrianfilip.booker.scaleaware.room

import com.adrianfilip.booker.domain.model.Room
import zio.json.{DeriveJsonCodec, JsonCodec}

sealed trait GetRoomResponse
object GetRoomResponse:

  case class GetRoomResponseSuccess(room: Room)                            extends GetRoomResponse
  case class GetRoomResponseFailure(code: String, message: Option[String]) extends GetRoomResponse

  object GetRoomResponseSuccess:
    implicit val codec: JsonCodec[GetRoomResponseSuccess] = DeriveJsonCodec.gen[GetRoomResponseSuccess]

  object GetRoomResponseFailure:
    implicit val codec: JsonCodec[GetRoomResponseFailure] = DeriveJsonCodec.gen[GetRoomResponseFailure]

  implicit val codec: JsonCodec[GetRoomResponse] = DeriveJsonCodec.gen[GetRoomResponse]
