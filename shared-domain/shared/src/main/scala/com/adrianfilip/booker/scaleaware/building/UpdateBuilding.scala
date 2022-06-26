package com.adrianfilip.booker.scaleaware.building

import zio.json.{DeriveJsonCodec, JsonCodec}

case class UpdateBuildingRequest(uuid: String, code: String, name: Option[String], address: Option[String])
object UpdateBuildingRequest:
  implicit val codec: JsonCodec[UpdateBuildingRequest] = DeriveJsonCodec.gen[UpdateBuildingRequest]

sealed trait UpdateBuildingResponse
object UpdateBuildingResponse:
  case class UpdateBuildingResponseSuccess()                                      extends UpdateBuildingResponse
  case class UpdateBuildingResponseFailure(code: String, message: Option[String]) extends UpdateBuildingResponse

  object UpdateBuildingResponseSuccess:
    implicit val codec: JsonCodec[UpdateBuildingResponseSuccess] = DeriveJsonCodec.gen[UpdateBuildingResponseSuccess]

  object UpdateBuildingResponseFailure:
    implicit val codec: JsonCodec[UpdateBuildingResponseFailure] = DeriveJsonCodec.gen[UpdateBuildingResponseFailure]

  implicit val codec: JsonCodec[UpdateBuildingResponse] = DeriveJsonCodec.gen[UpdateBuildingResponse]
