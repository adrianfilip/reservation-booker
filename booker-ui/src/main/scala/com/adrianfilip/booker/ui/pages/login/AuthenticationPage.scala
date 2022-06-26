package com.adrianfilip.booker.ui.pages.login

import com.adrianfilip.booker.scaleaware.authentication.{LoginRequest, LoginResult}
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.domain.components.NotificationsDiv.{notificationDivs, removeAllNotifications}
import com.adrianfilip.booker.ui.domain.model.{LoggedUserDetails, Notification}
import com.adrianfilip.booker.ui.domain.Event
import com.adrianfilip.booker.ui.pages.login.AuthenticationPage.LoginForm
import com.adrianfilip.booker.ui.services.{AuthenticationService, SecurityErrors, ServiceErrors}
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.thisEvents
import zio.ZIO

case class AuthenticationPage(
  loggedUserDetails: Var[Option[LoggedUserDetails]],
  loginService: AuthenticationService,
  loginNotifications: Var[List[Notification]]
)(implicit val runtime: zio.Runtime[Any]) {

  private val defaultLoginForm             = LoginForm("", "")
  private val loginFormVar: Var[LoginForm] = Var(defaultLoginForm)

  private def removeLoginNotifications = ZIO.attempt {
    removeAllNotifications(loginNotifications)
  }

  private def refreshLoginForm = loginFormVar.set(defaultLoginForm)

  def loginPage =
    div(
      float("left"),
      width("60%"),
      marginLeft("40%"),
      div(
        float("left"),
        width("100%"),
        div(
          marginTop("15%"),
          width("100%"),
          float("left"),
          div(
            float("left"),
            width("30%"),
            className("form-group"),
            input(
              className("form-control"),
              placeholder("username"),
              value <-- loginFormVar.signal.map(_.username),
              onChange.mapToValue --> { username => loginFormVar.set(loginFormVar.now().copy(username = username)) }
            )
          )
        ),
        div(
          float("left"),
          width("100%"),
          marginTop("2%"),
          div(
            float("left"),
            width("30%"),
            className("form-group"),
            input(
              className("form-control"),
              placeholder("password"),
              typ("password"),
              value <-- loginFormVar.signal.map(_.password),
              onChange.mapToValue --> { password => loginFormVar.set(loginFormVar.now().copy(password = password)) }
            )
          )
        ),
        div(
          float("left"),
          width("100%"),
          marginTop("4%"),
          div(
            marginLeft("10%"),
            button(
              "Login",
              className := "btn btn-primary",
              thisEvents(onClick)
                .flatMap(_ =>
                  zio.Unsafe
                    .unsafe {
                      runtime.unsafe
                        .run {
                          val loginForm = loginFormVar.now()
                          loginService
                            .login(LoginRequest(username = loginForm.username, password = loginForm.password))
                        }
                        .getOrThrowFiberFailure()
                    }
                ) --> {
                case LoginResult.SuccessfulLoginResult(user, token) =>
                  loggedUserDetails.set(Some(LoggedUserDetails(user, token)))
                  refreshLoginForm
                  zio.Unsafe
                    .unsafe {
                      runtime.unsafe
                        .run(removeLoginNotifications)
                        .getOrThrowFiberFailure()
                    }
                case LoginResult.FailedLoginResult(code)            =>
                  loginNotifications.set(List(Notification.Error(s"Login failed: $code")))
                case a: ServiceErrors.MalformedResult               =>
                  loginNotifications.set(List(Notification.Error(s"$code: ${a.reason.getOrElse("")}")))
                case a: ServiceErrors.UnexpectedFailure             =>
                  loginNotifications.set(List(Notification.Error(s"${a.code}: ${a.reason.getOrElse("")}")))
                case a: SecurityErrors                              =>
                  loginNotifications.set(List(Notification.Error(s"Login failed: ${a.code}")))
              }
            )
          )
        ),
        notificationDivs(loginNotifications)
      )
    )

}

object AuthenticationPage {
  case class LoginForm(username: String, password: String)

}
