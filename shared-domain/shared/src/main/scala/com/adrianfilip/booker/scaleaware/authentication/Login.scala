package com.adrianfilip.booker.scaleaware.authentication

import com.adrianfilip.booker.domain.model.User
import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}

case class LoginRequest(username: String, password: String)

object LoginRequest:
  implicit val decoder: JsonDecoder[LoginRequest] = DeriveJsonDecoder.gen[LoginRequest]
  implicit val encoder: JsonEncoder[LoginRequest] = DeriveJsonEncoder.gen[LoginRequest]

sealed trait LoginResult
object LoginResult:
  case class SuccessfulLoginResult(user: User, token: String) extends LoginResult
  case class FailedLoginResult(code: String)                  extends LoginResult

  object SuccessfulLoginResult:
    implicit val codec: JsonCodec[SuccessfulLoginResult] = DeriveJsonCodec.gen[SuccessfulLoginResult]

  object FailedLoginResult:
    implicit val codec: JsonCodec[FailedLoginResult] = DeriveJsonCodec.gen[FailedLoginResult]

  implicit val codec: JsonCodec[LoginResult] = DeriveJsonCodec.gen[LoginResult]
