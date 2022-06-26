package com.adrianfilip.booker.bk.infrastructure.authentication

import com.adrianfilip.booker.bk.OuterError
import com.adrianfilip.booker.bk.domain.AuthenticationService
import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError
import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError.{TokenExpired, TokenInvalid, UserNotFound}
import zhttp.http.{Headers, Request}
import zio.ZIO
import zio.json.*

object AuthenticationOperations:

  def bearerAuthZIO(token: String): ZIO[AuthenticationService, AuthenticationDomainError, Boolean] =
    for {
      authenticationService <- ZIO.service[AuthenticationService]
      _                     <- authenticationService
                                 .validToken(token)
                                 .tapError[AuthenticationService, AuthenticationDomainError](x => ZIO.succeed(println(x.toString)))
    } yield true

  def authenticate(request: Request): ZIO[
    AuthenticationService,
    OuterError.MissingAuthorizationToken | AuthenticationDomainError | UserNotFound,
    AuthenticationService.User
  ] =
    for {
      authenticationService <- ZIO.service[AuthenticationService]
      token                 <- ZIO
                                 .fromOption(request.headers.toList.find(kv => kv._1 == "Authorization"))
                                 .map(_._2.replace("Bearer ", ""))
                                 .orElseFail(OuterError.MissingAuthorizationToken(""))
      user                  <- authenticationService.authenticate(token)
    } yield user

  def parseRequest[A](request: Request)(implicit aDecoder: JsonDecoder[A]): ZIO[Any, OuterError.BadRequestError, A] =
    request.bodyAsString
      .flatMap(s => ZIO.fromEither(s.fromJson[A]))
      .mapError(s => OuterError.BadRequestError(s.toString))
