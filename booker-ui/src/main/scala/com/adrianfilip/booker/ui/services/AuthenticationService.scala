package com.adrianfilip.booker.ui.services

import com.adrianfilip.booker.domain.model.User
import com.adrianfilip.booker.scaleaware.authentication.{LoginRequest, LoginResult, LogoutRequest, LogoutResult}
import com.adrianfilip.booker.ui.services.HttpClient.{catchServiceErrors, handleResponse, makeRequest}
import com.raquo.airstream.core.EventStream
import io.laminext.fetch
import zio.{IO, Task, UIO, ZIO}
import zio.json.*

import java.util.UUID
import io.laminext.fetch.*

trait AuthenticationService:
  def login(m: LoginRequest): UIO[EventStream[LoginResult | ServiceErrors | SecurityErrors]]
  def logout(m: LogoutRequest, token: String): UIO[EventStream[LogoutResult | ServiceErrors | SecurityErrors]]

object AuthenticationService:

  val live =
    new AuthenticationService:
      override def login(m: LoginRequest): UIO[EventStream[LoginResult | ServiceErrors | SecurityErrors]] =
        makeRequest[LoginResult, LoginResult.SuccessfulLoginResult, LoginResult.FailedLoginResult](
          Fetch
            .post("http://localhost:8090/login", body = m.toJson)
        )

      override def logout(
        m: LogoutRequest,
        token: String
      ): UIO[EventStream[LogoutResult | ServiceErrors | SecurityErrors]] =
        makeRequest[LogoutResult, LogoutResult.SuccessLogoutResult, LogoutResult.FailedLogoutResult](
          Fetch.post(
            "http://localhost:8090/logout",
            body = m.toJson,
            headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
          )
        )

  val mockLoginService = new AuthenticationService:
    override def login(m: LoginRequest): UIO[EventStream[LoginResult | ServiceErrors | SecurityErrors]] =
      ZIO.succeed(
        EventStream.fromValue(
          LoginResult
            .SuccessfulLoginResult(user = User("", "", "", ""), token = UUID.randomUUID().toString)
        )
      )

    override def logout(m: LogoutRequest, token: String): UIO[EventStream[LogoutResult | ServiceErrors]] =
      ZIO.succeed(EventStream.fromValue(LogoutResult.SuccessLogoutResult()))
