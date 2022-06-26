package com.adrianfilip.booker.scaleaware.reservation

import com.adrianfilip.booker.domain.model.Floor
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.{LocalDate, LocalTime}

case class AddReservationRequest(
  seats: Int,
  date: LocalDate,
  duration: Int,
  startTime: LocalTime,
  endTime: LocalTime,
  buildingId: String,
  floor: Floor,
  roomId: String,
  holderId: String
)
object AddReservationRequest:
  implicit val codec: JsonCodec[AddReservationRequest] = DeriveJsonCodec.gen[AddReservationRequest]

sealed trait AddReservationResponse
object AddReservationResponse:
  final case class AddReservationResponseSuccess()                                      extends AddReservationResponse
  final case class AddReservationResponseFailure(code: String, message: Option[String]) extends AddReservationResponse

  object AddReservationResponseSuccess:
    implicit val codec: JsonCodec[AddReservationResponseSuccess] = DeriveJsonCodec.gen[AddReservationResponseSuccess]

  object AddReservationResponseFailure:
    implicit val codec: JsonCodec[AddReservationResponseFailure] = DeriveJsonCodec.gen[AddReservationResponseFailure]

  implicit val codec: JsonCodec[AddReservationResponse] = DeriveJsonCodec.gen[AddReservationResponse]
