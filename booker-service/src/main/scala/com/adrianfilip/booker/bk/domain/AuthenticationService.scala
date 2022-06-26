package com.adrianfilip.booker.bk.domain

import com.adrianfilip.booker.bk.domain.AuthenticationService.*
import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError
import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError.*
import zio.{IO, Task, ZIO}

trait AuthenticationService:
  def login(username: String, password: String): IO[InvalidPassword.type | UserNotFound, LoginResult]
  def validToken(token: String): ZIO[Any, AuthenticationDomainError, Unit]
  def authenticate(token: String): ZIO[Any, AuthenticationDomainError | UserNotFound, User]

object AuthenticationService:
  def login(
    username: String,
    password: String
  ): ZIO[AuthenticationService, InvalidPassword.type | UserNotFound, LoginResult] =
    ZIO.serviceWithZIO[AuthenticationService](_.login(username, password))

  def validToken(token: String): ZIO[AuthenticationService, AuthenticationDomainError, Unit] =
    ZIO.serviceWithZIO[AuthenticationService](_.validToken(token))

  def authenticate(token: String): ZIO[AuthenticationService, AuthenticationDomainError | UserNotFound, User] =
    ZIO.serviceWithZIO[AuthenticationService](_.authenticate(token))

  case class LoginResult(user: User, token: String)
  case class User(username: String, role: String, surname: String, lastName: String)
