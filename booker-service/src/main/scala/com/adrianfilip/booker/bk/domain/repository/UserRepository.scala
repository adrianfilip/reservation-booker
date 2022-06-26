package com.adrianfilip.booker.bk.domain.repository

import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError.UserNotFound
import com.adrianfilip.booker.bk.domain.repository.UserRepository.UserE
import zio.{IO, UIO, ZIO}

trait UserRepository:
  def findByUsername(username: String): UIO[Option[UserE]]
  def get(username: String): IO[UserNotFound, UserE]

object UserRepository:

  def findByUsername(username: String): ZIO[UserRepository, Nothing, Option[UserE]] =
    ZIO.serviceWithZIO(_.findByUsername(username))

  def get(username: String): ZIO[UserRepository, UserNotFound, UserE] =
    ZIO.serviceWithZIO(_.get(username))

  case class UserE(username: String, password: String, role: String, surname: String, lastName: String)
