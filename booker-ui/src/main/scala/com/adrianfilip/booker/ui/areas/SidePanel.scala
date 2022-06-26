package com.adrianfilip.booker.ui.areas

import com.adrianfilip.booker.ui.domain.Event
import com.raquo.laminar.api.L._

object SidePanel:

  def sidePanel(eventBus: EventBus[Event]) =
    val observer    = eventBus.toObserver
    val eventStream = eventBus.toObservable
    div(
      div(
        marginTop("10%"),
        marginLeft("10%"),
        width("90%"),
        onClick.mapToValue.map(_ => Event.SelectPageEvents.SelectedMyReservationsPage) --> observer,
        color <-- eventStream
          .collect { case a: Event.SelectPageEvents => a }
          .map(sc => if (sc == Event.SelectPageEvents.SelectedMyReservationsPage) "green" else ""),
        backgroundColor <-- eventStream
          .collect { case a: Event.SelectPageEvents => a }
          .map(sc => if (sc == Event.SelectPageEvents.SelectedMyReservationsPage) "lightyellow" else ""),
        "My reservations"
      ),
      div(
        marginTop("10%"),
        marginLeft("10%"),
        width("90%"),
        onClick.mapToValue.map(_ => Event.SelectPageEvents.SelectedLocationsPage) --> observer,
        color <-- eventStream
          .collect { case a: Event.SelectPageEvents => a }
          .map(sc => if (sc == Event.SelectPageEvents.SelectedLocationsPage) "green" else ""),
        backgroundColor <-- eventStream
          .collect { case a: Event.SelectPageEvents => a }
          .map(sc => if (sc == Event.SelectPageEvents.SelectedLocationsPage) "lightyellow" else ""),
        "Buildings/Rooms"
      )
    )
  