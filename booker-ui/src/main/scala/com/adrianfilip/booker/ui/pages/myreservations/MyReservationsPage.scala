package com.adrianfilip.booker.ui.pages.myreservations

import com.adrianfilip.booker.ui.domain.Event
import com.adrianfilip.booker.ui.domain.Event.MyReservationsPageEvents.{MakeReservationEvents, ReservationsTableEvents}
import com.adrianfilip.booker.ui.domain.Event.{SecurityEvent, SelectPageEvents}
import com.adrianfilip.booker.ui.domain.components.IsNot.*
import com.adrianfilip.booker.ui.domain.model.LoggedUserDetails
import com.adrianfilip.booker.ui.services.{BuildingsService, ReservationService, RoomsService}
import com.raquo.laminar.api.L.*

case class MyReservationsPage(
  appEventBus: EventBus[Event],
  reservationService: ReservationService,
  userDetails: LoggedUserDetails,
  buildingsService: BuildingsService,
  roomsService: RoomsService
)(implicit runtime: zio.Runtime[Any]):

  private val holderId = userDetails.user.username

  private def handleEvents(
    makeReservationEvents: EventStream[MakeReservationEvents | SecurityEvent],
    reservationsTableEventsObserver: Observer[ReservationsTableEvents | SecurityEvent]
  ) = List(makeReservationEvents.collect { case a: MakeReservationEvents.AddedReservation.type => a } --> { _ =>
    reservationsTableEventsObserver.onNext(ReservationsTableEvents.RefreshReservations(holderId))
  })

  def page =
    val makeReservationsEventsStream = appEventBus.events.collect[MakeReservationEvents | SecurityEvent] {
      case a: MakeReservationEvents => a
      case a: SecurityEvent         => a
    }
    val makeReservationsObserver     = appEventBus.writer.contracollect[MakeReservationEvents | SecurityEvent] {
      case a: MakeReservationEvents => a
      case a: SecurityEvent         => a
    }

    val reservationsTableEventStream = appEventBus.events.collect[ReservationsTableEvents | SecurityEvent] {
      case a: ReservationsTableEvents => a
      case a: SecurityEvent           => a
    }

    val reservationsTableObserver = appEventBus.writer.contracollect[ReservationsTableEvents | SecurityEvent] {
      case a: ReservationsTableEvents => a
      case a: SecurityEvent           => a
    }

    div(
      hidden <-- appEventBus.toObservable
        .collect { case a: SelectPageEvents => a }
        .isNot(Event.SelectPageEvents.SelectedMyReservationsPage),
      appEventBus.toObservable.collect { case a: Event.SelectPageEvents.SelectedMyReservationsPage.type => a } --> {
        _ =>
          makeReservationsObserver.onNext(MakeReservationEvents.Refresh)
          reservationsTableObserver.onNext(ReservationsTableEvents.RefreshReservations(holderId))
      },
      div(
        width("100%"),
        float("left"),
        MakeReservation(
          buildingsService = buildingsService,
          roomsService = roomsService,
          reservationService = reservationService,
          makeReservationEventStream = makeReservationsEventsStream,
          makeReservationEventObserver = makeReservationsObserver,
          userDetails = userDetails
        ).make
      ),
      div(
        width("100%"),
        float("left"),
        marginTop("3%"),
        MyReservationsTable(
          reservationsTableEvents = reservationsTableEventStream,
          reservationsTableObserver = reservationsTableObserver,
          reservationService = reservationService,
          userDetails = userDetails
        ).make
      ),
      handleEvents(makeReservationsEventsStream, reservationsTableObserver)
    )
