package com.adrianfilip.booker.scaleaware.reservation

import com.adrianfilip.booker.domain.model.Reservation
import zio.json.{DeriveJsonCodec, JsonCodec}

sealed trait GetReservationsResponse
object GetReservationsResponse:
  final case class GetReservationsResponseSuccess(reservations: List[Reservation])       extends GetReservationsResponse
  final case class GetReservationsResponseFailure(code: String, message: Option[String]) extends GetReservationsResponse

  object GetReservationsResponseSuccess:
    implicit val codec: JsonCodec[GetReservationsResponseSuccess] = DeriveJsonCodec.gen[GetReservationsResponseSuccess]

  object GetReservationsResponseFailure:
    implicit val codec: JsonCodec[GetReservationsResponseFailure] = DeriveJsonCodec.gen[GetReservationsResponseFailure]

  implicit val codec: JsonCodec[GetReservationsResponse] = DeriveJsonCodec.gen[GetReservationsResponse]
