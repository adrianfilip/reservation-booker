package com.adrianfilip.booker.bk.infrastructure.domain.repository
import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError
import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError.UserNotFound
import com.adrianfilip.booker.bk.domain.repository.UserRepository
import zio.{IO, UIO, ZIO, ZLayer}

object UserRepositoryMock:

  val mock =
    ZLayer.succeed(new UserRepository:

      val users = List(
        UserRepository
          .UserE(
            username = "john",
            password = "Gk2DSDOL2QVi8AF67qmSNA==",
            role = "admin",
            surname = "John",
            lastName = "Doe"
          ),
        UserRepository
          .UserE(
            username = "jane",
            password = "Gk2DSDOL2QVi8AF67qmSNA==",
            role = "admin",
            surname = "Jane",
            lastName = "Doe"
          )
      )

      override def findByUsername(username: String): UIO[Option[UserRepository.UserE]] =
        ZIO.succeed(users.find(_.username == username))
    )
