package com.adrianfilip.booker.bk.infrastructure

import com.adrianfilip.booker.bk.domain.DomainError.JWTTokenDomainError
import com.adrianfilip.booker.bk.infrastructure.JWTTokenService.Claim
import pdi.jwt.exceptions.JwtException
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.json.*
import zio.{IO, UIO, ZIO, ZLayer}

import java.time.Clock

object JWTTokenServiceLive {

  val live: ZLayer[java.time.Clock, Nothing, JWTTokenService] =
    ZLayer.fromZIO {
      for {
        clock <- ZIO.service[java.time.Clock]
      } yield new JWTTokenService:
        // Secret Authentication key
        val SECRET_KEY = "secretKey"

        implicit val javaClock: java.time.Clock = clock

        override def encode(username: String, role: String): UIO[String] =
          ZIO.attempt {
            val json  = Claim(username = username, role = role).toJson
            val claim = JwtClaim(content = json, subject = Some(username)).issuedNow.expiresIn(300)
            Jwt.encode(claim, SECRET_KEY, JwtAlgorithm.HS512)
          }.orDie

        override def decode(token: String): IO[JWTTokenDomainError, JwtClaim] =
          ZIO
            .fromTry {
              Jwt.decode(token, SECRET_KEY, Seq(JwtAlgorithm.HS512))
            }
            .mapError {
              case e if e.isInstanceOf[pdi.jwt.exceptions.JwtExpirationException] =>
                JWTTokenDomainError.ExpiredToken(e.getMessage)
              case e if e.isInstanceOf[JwtException]                              => JWTTokenDomainError.InvalidJWTToken(e.getMessage)
              case _                                                              => JWTTokenDomainError.MalformedToken
            }
    }

}
