package com.adrianfilip.booker.ui.pages.myreservations

import com.adrianfilip.booker.domain.model.Reservation
import com.adrianfilip.booker.scaleaware.reservation.{
  AddReservationResponse,
  CancelReservationResponse,
  GetReservationsResponse
}
import com.adrianfilip.booker.ui.domain.Event.MyReservationsPageEvents.ReservationsTableEvents
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.domain.components.NotificationsDiv.notificationDivs
import com.adrianfilip.booker.ui.domain.model.{LoggedUserDetails, Notification}
import com.adrianfilip.booker.ui.services.{ReservationService, SecurityErrors, ServiceErrors}
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.thisEvents
import zio.{Runtime, ZIO}

case class MyReservationsTable(
  reservationsTableEvents: EventStream[ReservationsTableEvents | SecurityEvent],
  reservationsTableObserver: Observer[ReservationsTableEvents | SecurityEvent],
  reservationService: ReservationService,
  userDetails: LoggedUserDetails
)(implicit runtime: zio.Runtime[Any]):

  private val reservationsVar: Var[List[Reservation]] = Var(List())

  private val componentNotifications: Var[List[Notification]]                                        = Var(List.empty)
  private val componentEventBus
    : EventBus[GetReservationsResponse | CancelReservationResponse | ServiceErrors | SecurityErrors] = new EventBus()

  private def componentEventBusHandler =
    List(
      componentEventBus.events
        .collect { case a: SecurityErrors =>
          SecurityEvent(a)
        } --> reservationsTableObserver,
      componentEventBus.events
        .collect { case a: ServiceErrors =>
          println(s"Service errors: $a")
          List(Notification.Error(s"${a.code}: ${a.reason.map(r => s": $r").getOrElse("")}"))
        } --> componentNotifications,
      componentEventBus.events.collect { case GetReservationsResponse.GetReservationsResponseSuccess(reservations) =>
        println(s"Reservations: $reservations")
        componentNotifications.set(List.empty)
        reservations
      } --> reservationsVar,
      componentEventBus.events.collect { case GetReservationsResponse.GetReservationsResponseFailure(code, message) =>
        println(s"Get reservations response failure: $code: $message")
        List(Notification.Error(s"$code: $message"))
      } --> componentNotifications,
      componentEventBus.events.collect { case _: CancelReservationResponse.CancelReservationResponseSuccess =>
        componentNotifications.set(List.empty)
        ReservationsTableEvents.RefreshReservations(userDetails.user.username)
      } --> reservationsTableObserver,
      componentEventBus.events.collect {
        case CancelReservationResponse.CancelReservationResponseFailure(code, message) =>
          List(Notification.Error(s"$code: $message"))
      } --> componentNotifications
    )

  private def reactToReservationsTableEvents =
    List(
      reservationsTableEvents
        .collect { case ReservationsTableEvents.RefreshReservations(holderId) =>
          holderId
        }
        .flatMap { holderId =>
          zio.Unsafe
            .unsafe {
              runtime.unsafe
                .run(reservationService.getReservations(holderId, userDetails.token))
                .getOrThrowFiberFailure()
            }
        } --> componentEventBus
    )

  def make =
    table(
      notificationDivs(componentNotifications),
      className("table table-hover"),
      thead(
        className("thead"),
        tr(
          th("Day"),
          th("Interval"),
          th("Building"),
          th("Floor"),
          th("Room"),
          th("Seats"),
          //the cancel button
          th("")
        )
      ),
      tbody(children <-- reservationsVar.toObservable.map { ls =>
        ls.map(reservation =>
          tr(
            width("100%"),
            td(reservation.date.toString()),
            td(reservation.startAt.toString() + "-" + reservation.endAt.toString()),
            td(reservation.building.code + reservation.building.name.map(" (" + _ + ")").getOrElse("")),
            td(reservation.floor.toString),
            td(reservation.room.code + reservation.room.name.map(" (" + _ + ")").getOrElse("")),
            td(reservation.seats),
            td(
              div(
                float("left"),
                button(
                  "Remove",
                  className("btn btn-danger"),
                  thisEvents(onClick).flatMap(_ =>
                    zio.Unsafe
                      .unsafe {
                        runtime.unsafe
                          .run(reservationService.cancelReservation(reservation.id, userDetails.token))
                          .getOrThrowFiberFailure()
                      }
                  ) --> componentEventBus
                )
              )
            )
          )
        )
      }),
      reactToReservationsTableEvents,
      componentEventBusHandler
    )
