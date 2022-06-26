package com.adrianfilip.booker.scaleaware.building

import zio.json.{DeriveJsonCodec, JsonCodec}

case class AddBuildingRequest(code: String, name: Option[String], address: Option[String])

object AddBuildingRequest:
  implicit val codec: JsonCodec[AddBuildingRequest] = DeriveJsonCodec.gen[AddBuildingRequest]

sealed trait AddBuildingResponse

object AddBuildingResponse:
  case class AddBuildingResponseSuccess() extends AddBuildingResponse

  case class AddBuildingResponseFailure(code: String, message: Option[String]) extends AddBuildingResponse

  object AddBuildingResponseSuccess:
    implicit val codec: JsonCodec[AddBuildingResponseSuccess] = DeriveJsonCodec.gen[AddBuildingResponseSuccess]

  object AddBuildingResponseFailure:
    implicit val codec: JsonCodec[AddBuildingResponseFailure] = DeriveJsonCodec.gen[AddBuildingResponseFailure]

  implicit val codec: JsonCodec[AddBuildingResponse] = DeriveJsonCodec.gen[AddBuildingResponse]
