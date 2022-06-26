package com.adrianfilip.booker.scaleaware.room

import com.adrianfilip.booker.domain.model.{Floor, Room}
import zio.json.{DeriveJsonCodec, JsonCodec}

case class GetFilteredRoomsRequest(
  code: Option[String],
  name: Option[String],
  floor: Option[Floor],
  seats: Option[Int],
  buildingId: Option[String]
)

object GetFilteredRoomsRequest:
  implicit val codec: JsonCodec[GetFilteredRoomsRequest] = DeriveJsonCodec.gen[GetFilteredRoomsRequest]

sealed trait GetFilteredRoomsResponse
object GetFilteredRoomsResponse:
  case class GetFilteredRoomsResponseSuccess(rooms: List[Room])                     extends GetFilteredRoomsResponse
  case class GetFilteredRoomsResponseFailure(code: String, message: Option[String]) extends GetFilteredRoomsResponse

  object GetFilteredRoomsResponseSuccess:
    implicit val codec: JsonCodec[GetFilteredRoomsResponseSuccess] =
      DeriveJsonCodec.gen[GetFilteredRoomsResponseSuccess]

  object GetFilteredRoomsResponseFailure:
    implicit val codec: JsonCodec[GetFilteredRoomsResponseFailure] =
      DeriveJsonCodec.gen[GetFilteredRoomsResponseFailure]

  implicit val codec: JsonCodec[GetFilteredRoomsResponse] = DeriveJsonCodec.gen[GetFilteredRoomsResponse]
