package com.adrianfilip.booker.ui.domain

import com.adrianfilip.booker.domain.model.{Building, Floor}
import com.adrianfilip.booker.ui.services.{SecurityErrors, ServiceErrors}

sealed trait Event

object Event:

  final case class SecurityEvent(ev: SecurityErrors) extends Event
  sealed trait LocationsPageEvents                   extends Event
  sealed trait MyReservationsPageEvents              extends Event
  sealed trait SelectPageEvents                      extends Event

  object SelectPageEvents:
    case object SelectedLocationsPage      extends SelectPageEvents
    case object SelectedMyReservationsPage extends SelectPageEvents

  object LocationsPageEvents:
    sealed trait SelectTabEvents extends LocationsPageEvents
    sealed trait BuildingsEvents extends LocationsPageEvents
    sealed trait RoomsEvents     extends LocationsPageEvents

    object SelectTabEvents:
      case object SelectBuildingsTab extends SelectTabEvents
      case object SelectRoomsTab     extends SelectTabEvents

    object BuildingsEvents:
      case object RefreshBuildingsWithFilters extends BuildingsEvents

    object RoomsEvents:
      case object RefreshRoomsWithFilters extends RoomsEvents
      case object RefreshBuildings        extends RoomsEvents

  object MyReservationsPageEvents:

    sealed trait MakeReservationEvents   extends MyReservationsPageEvents
    sealed trait ReservationsTableEvents extends MyReservationsPageEvents

    object MakeReservationEvents:
      case object Refresh          extends MakeReservationEvents
      case object AddedReservation extends MakeReservationEvents

    object ReservationsTableEvents:
      final case class RefreshReservations(holderId: String) extends ReservationsTableEvents
