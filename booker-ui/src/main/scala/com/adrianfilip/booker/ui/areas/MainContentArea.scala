package com.adrianfilip.booker.ui.areas

import com.adrianfilip.booker.ui.domain.Event
import com.adrianfilip.booker.ui.domain.model.LoggedUserDetails
import com.adrianfilip.booker.ui.pages.locations.LocationsPage
import com.adrianfilip.booker.ui.pages.myreservations.MyReservationsPage
import com.adrianfilip.booker.ui.services.{BuildingsService, ReservationService, RoomsService}
import com.raquo.laminar.api.L.*

case class MainContentArea(
  backendService: BuildingsService,
  roomsService: RoomsService,
  appEventBus: EventBus[Event],
  reservationService: ReservationService,
  buildingsService: BuildingsService,
  userDetails: LoggedUserDetails
)(implicit runtime: zio.Runtime[Any]):

  def mainContentDiv = div(
    MyReservationsPage(
      appEventBus = appEventBus,
      reservationService = reservationService,
      userDetails = userDetails,
      buildingsService = buildingsService,
      roomsService = roomsService
    ).page,
    LocationsPage(
      buildingsService = backendService,
      roomsService = roomsService,
      appEventBus = appEventBus,
      userDetails = userDetails
    ).page
  )
