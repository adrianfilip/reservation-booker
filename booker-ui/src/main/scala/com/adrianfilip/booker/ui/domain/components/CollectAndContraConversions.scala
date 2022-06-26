package com.adrianfilip.booker.ui.domain.components

import com.adrianfilip.booker.ui.domain.Event
import com.adrianfilip.booker.ui.domain.Event.LocationsPageEvents.{BuildingsEvents, RoomsEvents, SelectTabEvents}
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.services.ServiceErrors

object CollectAndContraConversions:

  val collectBuildingsEvents: PartialFunction[Event, BuildingsEvents | SecurityEvent]       = {
    case a: BuildingsEvents => a
    case a: SecurityEvent   => a
  }
  val contracollectBuildingsEvents: PartialFunction[BuildingsEvents | SecurityEvent, Event] = {
    case a: BuildingsEvents => a
    case a: SecurityEvent   => a
  }

  val collectRoomsEvents: PartialFunction[Event, RoomsEvents | SecurityEvent]       = {
    case a: RoomsEvents   => a
    case a: SecurityEvent => a
  }
  val contracollectRoomsEvents: PartialFunction[RoomsEvents | SecurityEvent, Event] = {
    case a: RoomsEvents   => a
    case a: SecurityEvent => a
  }

  val collectSelectTabEvents: PartialFunction[Event, SelectTabEvents]       = { case a: SelectTabEvents => a }
  val contracollectSelectTabEvents: PartialFunction[SelectTabEvents, Event] = { case a: SelectTabEvents => a }
