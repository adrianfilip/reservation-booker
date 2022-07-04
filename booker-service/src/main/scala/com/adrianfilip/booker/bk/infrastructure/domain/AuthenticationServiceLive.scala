package com.adrianfilip.booker.bk.infrastructure.domain

import com.adrianfilip.booker.bk.domain.AuthenticationService.{LoginResult, User}
import com.adrianfilip.booker.bk.domain.DomainError.{AuthenticationDomainError, JWTTokenDomainError}
import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError.*
import com.adrianfilip.booker.bk.infrastructure.JWTTokenService.Claim
import com.adrianfilip.booker.bk.domain.repository.UserRepository
import com.adrianfilip.booker.bk.domain.repository.UserRepository.UserE
import com.adrianfilip.booker.bk.domain.AuthenticationService
import com.adrianfilip.booker.bk.infrastructure.{EncryptionService, JWTTokenService}
import pdi.jwt.JwtClaim
import zio.Clock.ClockJava
import zio.json.*
import zio.{IO, ZIO, ZLayer}

private[infrastructure] case class AuthenticationServiceLive(
  tokenService: JWTTokenService,
  userRepository: UserRepository,
  encryptionService: EncryptionService
) extends AuthenticationService:

  private val secret = "secret"

  def toModel(userE: UserE): User =
    User(username = userE.username, role = userE.role, surname = userE.surname, lastName = userE.lastName)

  override def login(username: String, password: String): IO[InvalidPassword.type | UserNotFound, LoginResult] =
    for {
      user <- userRepository.get(username)

      encryptedPassword <- encryptionService.encrypt(password, secret)
      _                 <- ZIO.when(!user.password.equals(encryptedPassword))(IO.fail(InvalidPassword))
      token             <- tokenService.encode(username, user.role)

    } yield LoginResult(user = toModel(user), token = token)

  override def validToken(token: String): ZIO[Any, AuthenticationDomainError, Unit] =
    jwtClaim(token).unit

  private def jwtClaim(token: String): ZIO[Any, AuthenticationDomainError, JwtClaim] =
    tokenService
      .decode(token)
      .mapError {
        case JWTTokenDomainError.MalformedToken           => TokenInvalid("token.malformed")
        case JWTTokenDomainError.ExpiredToken(message)    => TokenExpired(message)
        case JWTTokenDomainError.InvalidJWTToken(message) => TokenInvalid(message)
      }

  private def claim(token: String): ZIO[Any, AuthenticationDomainError, Claim] =
    for
      jwtClaim <- jwtClaim(token)
      claim    <-
        ZIO.fromEither(jwtClaim.content.fromJson[Claim]).orElseFail(TokenInvalid("token.claim.malformed"))
    yield claim

  override def authenticate(token: String): ZIO[Any, AuthenticationDomainError | UserNotFound, User] = {
    for {
      claim <- claim(token)
      user  <- userRepository.get(claim.username)
    } yield toModel(user)
  }

object AuthenticationServiceLive:

  val layer: ZLayer[
    java.time.Clock with JWTTokenService with UserRepository with EncryptionService,
    Nothing,
    AuthenticationService
  ] =
    ZLayer {
      for {
        tokenService      <- ZIO.service[JWTTokenService]
        userRepository    <- ZIO.service[UserRepository]
        encryptionService <- ZIO.service[EncryptionService]
      } yield AuthenticationServiceLive(tokenService, userRepository, encryptionService)
    }
