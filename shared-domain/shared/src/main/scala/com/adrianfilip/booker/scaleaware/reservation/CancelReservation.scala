package com.adrianfilip.booker.scaleaware.reservation

import zio.json.{DeriveJsonCodec, JsonCodec}

sealed trait CancelReservationResponse
object CancelReservationResponse:
  final case class CancelReservationResponseSuccess() extends CancelReservationResponse
  final case class CancelReservationResponseFailure(code: String, message: Option[String])
      extends CancelReservationResponse

  object CancelReservationResponseSuccess:
    implicit val codec: JsonCodec[CancelReservationResponseSuccess] =
      DeriveJsonCodec.gen[CancelReservationResponseSuccess]

  object CancelReservationResponseFailure:
    implicit val codec: JsonCodec[CancelReservationResponseFailure] =
      DeriveJsonCodec.gen[CancelReservationResponseFailure]

  implicit val codec: JsonCodec[CancelReservationResponse] = DeriveJsonCodec.gen[CancelReservationResponse]
