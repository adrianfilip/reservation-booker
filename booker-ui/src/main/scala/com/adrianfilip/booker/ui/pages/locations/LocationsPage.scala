package com.adrianfilip.booker.ui.pages.locations

import com.adrianfilip.booker.ui.CSS.LocationsCSS
import com.adrianfilip.booker.ui.domain.Event
import com.adrianfilip.booker.ui.domain.Event.SelectPageEvents
import com.adrianfilip.booker.ui.domain.components.IsNot.*
import com.adrianfilip.booker.ui.domain.components.CollectAndContraConversions.*
import com.adrianfilip.booker.ui.domain.model.LoggedUserDetails
import com.adrianfilip.booker.ui.pages.locations.tabs.TabSelection
import com.adrianfilip.booker.ui.pages.locations.tabs.buildings.BuildingsTab
import com.adrianfilip.booker.ui.pages.locations.tabs.rooms.RoomsTab
import com.adrianfilip.booker.ui.services.{BuildingsService, RoomsService}
import com.raquo.laminar.api.L.*

case class LocationsPage(
  buildingsService: BuildingsService,
  roomsService: RoomsService,
  appEventBus: EventBus[Event],
  userDetails: LoggedUserDetails
)(implicit runtime: zio.Runtime[Any]):

  val locationsCSS: LocationsCSS.type = LocationsCSS

  def page =
    div(
      hidden <-- appEventBus.toObservable
        .collect { case a: SelectPageEvents => a }
        .isNot(Event.SelectPageEvents.SelectedLocationsPage),
      TabSelection(
        appEventBus.events.collect(collectSelectTabEvents),
        appEventBus.writer.contracollect(contracollectSelectTabEvents)
      ).make,
      div(
        RoomsTab(
          roomsService = roomsService,
          buildingsService = buildingsService,
          eventStream = appEventBus.events.collect(collectRoomsEvents),
          observer = appEventBus.writer.contracollect(contracollectRoomsEvents),
          selectedTabsEventStream = appEventBus.events.collect(collectSelectTabEvents),
          userDetails = userDetails
        ).make,
        BuildingsTab(
          backendService = buildingsService,
          eventStream = appEventBus.events.collect(collectBuildingsEvents),
          observer = appEventBus.writer.contracollect(contracollectBuildingsEvents),
          selectedTabsEventStream = appEventBus.events.collect(collectSelectTabEvents),
          userDetails = userDetails
        ).buildingsTabDiv,
        onMountCallback(_ => appEventBus.writer.onNext(Event.LocationsPageEvents.SelectTabEvents.SelectRoomsTab))
      )
    )
