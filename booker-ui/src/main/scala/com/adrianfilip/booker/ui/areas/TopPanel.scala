package com.adrianfilip.booker.ui.areas

import com.adrianfilip.booker.scaleaware.authentication.{LogoutRequest, LogoutResult}
import com.adrianfilip.booker.ui.domain.Event
import com.adrianfilip.booker.ui.domain.model.LoggedUserDetails
import com.adrianfilip.booker.ui.services.{AuthenticationService, SecurityErrors, ServiceErrors}
import com.raquo.laminar.api.L.*
import io.laminext.fetch.Fetch
import io.laminext.syntax.core.thisEvents

case class TopPanel(
  loggedUserDetailsVar: Var[Option[LoggedUserDetails]],
  loggedUserDetails: LoggedUserDetails,
  loginService: AuthenticationService,
  appEventBus: EventBus[Event]
)(implicit runtime: zio.Runtime[Any]):

  //add handler here

  def topPanel = div(
    width("100%"),
    float("left"),
    div(
      width("100%"),
      float("left"),
      div(width("90%"), float("left"), "Welcome " + loggedUserDetails.user.username + "!"),
      div(
        width("10%"),
        float("left"),
        button(
          "Logout",
          className := "btn btn-danger",
          thisEvents(onClick)
            .flatMap[Any] { _ =>
              zio.Unsafe.unsafe {
                runtime.unsafe
                  .run(loginService.logout(LogoutRequest(loggedUserDetails.user.username), loggedUserDetails.token))
                  .getOrThrowFiberFailure()
              }
            }
            .map {
              case a: LogoutResult.SuccessLogoutResult => println(a)
              case a: LogoutResult.FailedLogoutResult  => println(a)
              case a: SecurityErrors                   => appEventBus.writer.onNext(Event.SecurityEvent(a))
              case a: ServiceErrors.UnexpectedFailure  => println(a)
              case a: ServiceErrors.MalformedResult    => println(a)
            }
            .map(_ => None) --> loggedUserDetailsVar
        )
      )
    )
  )
