package com.adrianfilip.booker.bk.infrastructure.domain

import com.adrianfilip.booker.bk.domain.AuthenticationService.{LoginResult, User}
import com.adrianfilip.booker.bk.domain.DomainError.{AuthenticationDomainError, JWTTokenDomainError}
import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError.*
import com.adrianfilip.booker.bk.infrastructure.JWTTokenService.Claim
import com.adrianfilip.booker.bk.domain.repository.UserRepository
import com.adrianfilip.booker.bk.domain.repository.UserRepository.UserE
import com.adrianfilip.booker.bk.domain.AuthenticationService
import com.adrianfilip.booker.bk.infrastructure.{EncryptionService, JWTTokenService}
import zio.Clock.ClockJava
import zio.json.*
import zio.{IO, ZIO, ZLayer}

object AuthenticationServiceLive:

  val live: ZLayer[
    java.time.Clock with JWTTokenService with UserRepository with EncryptionService,
    Nothing,
    AuthenticationService
  ] =
    ZLayer.fromZIO(for {
      tokenService      <- ZIO.service[JWTTokenService]
      userRepository    <- ZIO.service[UserRepository]
      encryptionService <- ZIO.service[EncryptionService]
    } yield new AuthenticationService {

      private val secret = "secret"

      def toModel(userE: UserE): User =
        User(username = userE.username, role = userE.role, surname = userE.surname, lastName = userE.lastName)

      override def login(username: String, password: String): IO[InvalidPassword.type | UserNotFound, LoginResult] =
        for {
          userOp            <- userRepository.findByUsername(username)
          _                 <- ZIO.when(userOp.isEmpty)(IO.fail(UserNotFound(username)))
          user               = userOp.get
          encryptedPassword <- encryptionService.encrypt(password, secret)
          _                 <- ZIO.when(!user.password.equals(encryptedPassword))(IO.fail(InvalidPassword))
          token             <- tokenService.encode(username, user.role)

        } yield LoginResult(user = toModel(user), token = token)

      override def validToken(token: String): ZIO[Any, AuthenticationDomainError, Unit] =
        for {
          _ <-
            tokenService
              .decode(token)
              .mapError {
                case JWTTokenDomainError.MalformedToken           => TokenInvalid("token.malformed")
                case JWTTokenDomainError.ExpiredToken(message)    => TokenExpired(message)
                case JWTTokenDomainError.InvalidJWTToken(message) => TokenInvalid(message)
              }
        } yield ()

      override def authenticate(token: String): ZIO[Any, AuthenticationDomainError | UserNotFound, User] = {
        for {
          validatedToken <-
            tokenService
              .decode(token)
              .mapError {
                case JWTTokenDomainError.MalformedToken           => TokenInvalid("token.malformed")
                case JWTTokenDomainError.ExpiredToken(message)    => TokenExpired(message)
                case JWTTokenDomainError.InvalidJWTToken(message) => TokenInvalid(message)
              }
          tokenContent   <-
            ZIO.fromEither(validatedToken.content.fromJson[Claim]).orElseFail(TokenInvalid("token.claim.malformed"))
          user           <- userRepository
                              .get(tokenContent.username)
        } yield toModel(user)
      }

    })
