package com.adrianfilip.booker.bk.infrastructure

import com.adrianfilip.booker.bk.domain.DomainError.JWTTokenDomainError
import com.adrianfilip.booker.bk.infrastructure.JWTTokenService.Claim
import pdi.jwt.exceptions.JwtException
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.json.*
import zio.{Clock, IO, UIO, ZIO, ZLayer}

private[infrastructure] case class JWTTokenServiceLive(clock: Clock, secretKey: String) extends JWTTokenService:

  override def encode(username: String, role: String): UIO[String] =
    for
      now      <- clock.instant
      issuedAt  = now.getEpochSecond
      expiresAt = now.plusSeconds(300).getEpochSecond
      token    <- ZIO.attempt {
                    val json  = Claim(username = username, role = role).toJson
                    val claim =
                      JwtClaim(content = json, subject = Some(username)).issuedAt(issuedAt).expiresAt(expiresAt)
                    Jwt.encode(claim, secretKey, JwtAlgorithm.HS512)
                  }.orDie
    yield token

  override def decode(token: String): IO[JWTTokenDomainError, JwtClaim] =
    ZIO
      .fromTry {
        Jwt.decode(token, secretKey, Seq(JwtAlgorithm.HS512))
      }
      .mapError {
        case e if e.isInstanceOf[pdi.jwt.exceptions.JwtExpirationException] =>
          JWTTokenDomainError.ExpiredToken(e.getMessage)
        case e if e.isInstanceOf[JwtException]                              => JWTTokenDomainError.InvalidJWTToken(e.getMessage)
        case _                                                              => JWTTokenDomainError.MalformedToken
      }

object JWTTokenServiceLive:

  def layer(secretKey: String = "secretKey"): ZLayer[Clock, Nothing, JWTTokenService] =
    ZLayer {
      for {
        clock <- ZIO.service[Clock]
      } yield JWTTokenServiceLive(clock, secretKey)
    }
