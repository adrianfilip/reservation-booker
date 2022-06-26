package com.adrianfilip.booker.bk.infrastructure

import com.adrianfilip.booker.bk.domain.DomainError.JWTTokenDomainError
import com.adrianfilip.booker.bk.domain.DomainError.JWTTokenDomainError.*
import pdi.jwt.JwtClaim
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.{IO, UIO, ZIO}

trait JWTTokenService:
  def encode(username: String, role: String): UIO[String]
  def decode(token: String): IO[JWTTokenDomainError, JwtClaim]

object JWTTokenService:

  def encode(username: String, role: String): ZIO[JWTTokenService, Nothing, String] =
    ZIO.serviceWithZIO(_.encode(username, role))

  def decode(token: String): ZIO[JWTTokenService, JWTTokenDomainError, JwtClaim] =
    ZIO.serviceWithZIO(_.decode(token))

  case class Claim(username: String, role: String)

  object Claim
  implicit val decoder: JsonDecoder[Claim] = DeriveJsonDecoder.gen[Claim]
  implicit val encoder: JsonEncoder[Claim] = DeriveJsonEncoder.gen[Claim]
