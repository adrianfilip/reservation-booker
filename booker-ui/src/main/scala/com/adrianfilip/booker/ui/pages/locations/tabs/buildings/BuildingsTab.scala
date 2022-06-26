package com.adrianfilip.booker.ui.pages.locations.tabs.buildings

import com.adrianfilip.booker.scaleaware.building.GetFilteredBuildingsRequest
import com.adrianfilip.booker.ui.domain.Event.LocationsPageEvents.{BuildingsEvents, SelectTabEvents}
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.domain.model.LoggedUserDetails
import com.adrianfilip.booker.ui.services.{BuildingsService, ServiceErrors}
import com.raquo.laminar.api.L.*

case class BuildingsTab(
  backendService: BuildingsService,
  eventStream: EventStream[BuildingsEvents | SecurityEvent],
  selectedTabsEventStream: EventStream[SelectTabEvents],
  observer: Observer[BuildingsEvents | SecurityEvent],
  userDetails: LoggedUserDetails
)(implicit runtime: zio.Runtime[Any]):

  val filters: Var[GetFilteredBuildingsRequest] = Var(GetFilteredBuildingsRequest(None, None, None))

  def buildingsTabDiv =
    div(
      className("tabContent"),
      hidden <-- selectedTabsEventStream
        .map(_ == SelectTabEvents.SelectBuildingsTab)
        .map(!_),
      selectedTabsEventStream.collect { case a: SelectTabEvents.SelectBuildingsTab.type => a } --> { _ =>
        observer.onNext(BuildingsEvents.RefreshBuildingsWithFilters)
      },
      BuildingFilterAddDiv(
        filters = filters,
        buildingsService = backendService,
        observer = observer,
        userDetails = userDetails
      ).make,
      div(
        className("buildingsTableDiv"),
        BuildingsTable(backendService, observer, eventStream, filters, userDetails).make
      )
    )
