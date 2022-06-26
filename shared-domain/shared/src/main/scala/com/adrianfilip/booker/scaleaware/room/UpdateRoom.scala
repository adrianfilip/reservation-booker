package com.adrianfilip.booker.scaleaware.room

import com.adrianfilip.booker.domain.model.{Floor, Room}
import zio.json.{DeriveJsonCodec, JsonCodec}

case class UpdateRoomRequest(
  uuid: String,
  code: String,
  seats: Int,
  name: Option[String],
  floor: Floor,
  buildingId: String
)
object UpdateRoomRequest:
  implicit val codec: JsonCodec[UpdateRoomRequest] = DeriveJsonCodec.gen[UpdateRoomRequest]

sealed trait UpdateRoomResponse
object UpdateRoomResponse:
  case class UpdateRoomResponseSuccess()                                      extends UpdateRoomResponse
  case class UpdateRoomResponseFailure(code: String, message: Option[String]) extends UpdateRoomResponse

  object UpdateRoomResponseSuccess:
    implicit val codec: JsonCodec[UpdateRoomResponseSuccess] = DeriveJsonCodec.gen[UpdateRoomResponseSuccess]

  object UpdateRoomResponseFailure:
    implicit val codec: JsonCodec[UpdateRoomResponseFailure] = DeriveJsonCodec.gen[UpdateRoomResponseFailure]

  implicit val codec: JsonCodec[UpdateRoomResponse] = DeriveJsonCodec.gen[UpdateRoomResponse]
