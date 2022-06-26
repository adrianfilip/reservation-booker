package com.adrianfilip.booker.ui

import com.adrianfilip.booker.domain.model.User
import com.adrianfilip.booker.scaleaware.authentication.LoginResult
import com.adrianfilip.booker.ui.CSS.{Bootstrap4CSS, RootCSS}
import com.adrianfilip.booker.ui.areas.BottomPanel.bottomPanel
import com.adrianfilip.booker.ui.areas.SidePanel.sidePanel
import com.adrianfilip.booker.ui.domain.Event
import com.adrianfilip.booker.ui.pages.login.AuthenticationPage
import com.adrianfilip.booker.ui.areas.{MainContentArea, TopPanel}
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.domain.model.{LoggedUserDetails, Notification}
import com.adrianfilip.booker.ui.services.{
  AuthenticationService,
  BuildingsService,
  ReservationService,
  RoomsService,
  SecurityErrors
}
import com.raquo.laminar.api.L.*

object Root:

  implicit val runtime: zio.Runtime[Any] = zio.Runtime.default

  val bootstrap4CSS: Bootstrap4CSS.type = Bootstrap4CSS
  val rootCSS: RootCSS.type             = RootCSS

  val buildingsService   = BuildingsService.live
  val roomsService       = RoomsService.mockRoomsService
  val reservationService = ReservationService.live
  val loginService       = AuthenticationService.live

  val appEventBus: EventBus[Event] = new EventBus[Event]()

  val loggedUserDetailsVar: Var[Option[LoggedUserDetails]] = Var(None)

  private val loginNotifications: Var[List[Notification]] = Var(List.empty)

  private def handleEvents =
    List(appEventBus.events.collect { case SecurityEvent(se) => se } --> {
      case SecurityErrors.Forbidden(status, code, reason)    =>
        println(s"Forbidden: $status $code $reason")
        loggedUserDetailsVar.set(None)
        loginNotifications.set(
          List(
            Notification.Warning(s"You were redirected to this page because you are forbidden! $code"),
            Notification.Warning("Please login with a user with the desired access!")
          )
        )
      case SecurityErrors.Unauthorized(status, code, reason) =>
        println(s"Unauthorized: $status $code $reason")
        loggedUserDetailsVar.set(None)
        loginNotifications.set(
          List(Notification.Warning(s"You were redirected to this page because you are unauthorized! $code"))
        )
    })

  def root =
    div(
      child <-- loggedUserDetailsVar.signal.map {
        case Some(loggedUserDetails) =>
          div(
            div(
              className("topPanel"),
              TopPanel(loggedUserDetailsVar, loggedUserDetails, loginService, appEventBus).topPanel
            ),
            div(
              className("middleArea"),
              div(className("sidePanel"), sidePanel(appEventBus)),
              div(
                className("contentArea"),
                MainContentArea(
                  buildingsService,
                  roomsService,
                  appEventBus,
                  reservationService,
                  buildingsService,
                  loggedUserDetails
                ).mainContentDiv
              ),
              //I put it after the contentArea otherwise the emitted event will not be consumed there
              onMountCallback(_ => appEventBus.writer.onNext(Event.SelectPageEvents.SelectedMyReservationsPage))
            ),
            div(className("bottomPanel"), bottomPanel)
          )
        case None                    =>
          AuthenticationPage(loggedUserDetailsVar, loginService, loginNotifications).loginPage
      },
      handleEvents
    )
