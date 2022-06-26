package com.adrianfilip.booker.scaleaware.room

import com.adrianfilip.booker.domain.model.{Floor, Room}
import zio.json.{DeriveJsonCodec, JsonCodec}

case class AddRoomRequest(code: String, seats: Int, name: Option[String], floor: Floor, buildingId: String)
object AddRoomRequest:
  implicit val codec: JsonCodec[AddRoomRequest] = DeriveJsonCodec.gen[AddRoomRequest]

sealed trait AddRoomResponse
object AddRoomResponse:
  case class AddRoomResponseSuccess()                                      extends AddRoomResponse
  case class AddRoomResponseFailure(code: String, message: Option[String]) extends AddRoomResponse

  object AddRoomResponseSuccess:
    implicit val codec: JsonCodec[AddRoomResponseSuccess] = DeriveJsonCodec.gen[AddRoomResponseSuccess]

  object AddRoomResponseFailure:
    implicit val codec: JsonCodec[AddRoomResponseFailure] = DeriveJsonCodec.gen[AddRoomResponseFailure]

  implicit val codec: JsonCodec[AddRoomResponse] = DeriveJsonCodec.gen[AddRoomResponse]
