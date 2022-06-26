package com.adrianfilip.booker.scaleaware.authentication

import zio.json.{DeriveJsonCodec, JsonCodec}

case class LogoutRequest(username: String)
object LogoutRequest:
  implicit val codec: JsonCodec[LogoutRequest] = DeriveJsonCodec.gen[LogoutRequest]

sealed trait LogoutResult
object LogoutResult:
  case class SuccessLogoutResult()            extends LogoutResult
  case class FailedLogoutResult(code: String) extends LogoutResult

  object SuccessLogoutResult:
    implicit val codec: JsonCodec[SuccessLogoutResult] = DeriveJsonCodec.gen[SuccessLogoutResult]

  object FailedLogoutResult:
    implicit val codec: JsonCodec[FailedLogoutResult] = DeriveJsonCodec.gen[FailedLogoutResult]

  implicit val codec: JsonCodec[LogoutResult] = DeriveJsonCodec.gen[LogoutResult]
