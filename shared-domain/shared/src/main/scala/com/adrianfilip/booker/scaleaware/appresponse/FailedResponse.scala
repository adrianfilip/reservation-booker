package com.adrianfilip.booker.scaleaware.appresponse

import zio.json.*

final case class FailedResponse(code: String, details: Option[String])

object FailedResponse:
  implicit val codec: JsonCodec[FailedResponse] = DeriveJsonCodec.gen[FailedResponse]
