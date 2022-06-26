package com.adrianfilip.booker.ui.pages.locations.tabs.rooms

import com.adrianfilip.booker.domain.model.Building
import com.adrianfilip.booker.scaleaware.room.GetFilteredRoomsRequest
import com.adrianfilip.booker.ui.domain.Event.LocationsPageEvents.{RoomsEvents, SelectTabEvents}
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.services.{BuildingsService, RoomsService}
import com.raquo.laminar.api.L.*
import com.adrianfilip.booker.ui.domain.components.IsNot.*
import com.adrianfilip.booker.ui.domain.model.LoggedUserDetails

case class RoomsTab(
  roomsService: RoomsService,
  buildingsService: BuildingsService,
  eventStream: EventStream[RoomsEvents | SecurityEvent],
  selectedTabsEventStream: EventStream[SelectTabEvents],
  observer: Observer[RoomsEvents | SecurityEvent],
  userDetails: LoggedUserDetails
)(implicit val runtime: zio.Runtime[Any]):

  val filters: Var[GetFilteredRoomsRequest] = Var(
    GetFilteredRoomsRequest(code = None, name = None, floor = None, seats = None, buildingId = None)
  )

  private val availableBuildings: Var[List[Building]] = Var(List.empty)

  def make =
    div(
      className("tabContent"),
      hidden <-- selectedTabsEventStream
        .isNot(SelectTabEvents.SelectRoomsTab),
      selectedTabsEventStream.collect { case a: SelectTabEvents.SelectRoomsTab.type => a } --> { _ =>
        observer.onNext(RoomsEvents.RefreshBuildings)
        observer.onNext(RoomsEvents.RefreshRoomsWithFilters)
      },
      RoomsFilterAdd(
        filters = filters,
        roomsService = roomsService,
        roomsEventsStream = eventStream,
        roomsEventsObserver = observer,
        buildingsService = buildingsService,
        availableBuildings = availableBuildings,
        userDetails = userDetails
      ).make,
      div(
        className("roomsTableDiv"),
        RoomsTable(
          roomsService = roomsService,
          roomsEventsObserver = observer,
          roomsEventsStream = eventStream,
          filters = filters,
          availableBuildings = availableBuildings,
          userDetails = userDetails
        ).make
      )
    )
