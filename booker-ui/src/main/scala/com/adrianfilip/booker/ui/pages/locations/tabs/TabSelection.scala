package com.adrianfilip.booker.ui.pages.locations.tabs

import com.adrianfilip.booker.ui.domain.Event.LocationsPageEvents
import com.raquo.laminar.api.L._

case class TabSelection(
  eventStream: EventStream[LocationsPageEvents.SelectTabEvents],
  observer: Observer[LocationsPageEvents.SelectTabEvents]
):

  def make = div(
    ul(
      className("nav", "nav-tabs", "nav-fill"),
      li(
        className("nav-item"),
        navTabLink("Rooms", LocationsPageEvents.SelectTabEvents.SelectRoomsTab, eventStream, observer)
      ),
      li(
        className("nav-item"),
        navTabLink("Buildings", LocationsPageEvents.SelectTabEvents.SelectBuildingsTab, eventStream, observer)
      )
    ),
    borderTopColor("transparent"),
    borderBottomColor("grey"),
    borderLeftColor("transparent"),
    borderRightColor("transparent"),
    borderStyle("solid")
  )

  def navTabLink[T, U >: T](label: String, selectedTab: T, eventStream: EventStream[U], observer: Observer[U]) =
    a(
      className("nav-link"),
      className <-- eventStream.map(st => if (st == selectedTab) "active" else ""),
      color <-- eventStream.map(st => if (st == selectedTab) "green" else "black"),
      backgroundColor <-- eventStream.map(st => if (st == selectedTab) "lightyellow" else ""),
      onClick.map(_ => selectedTab) --> observer,
      href("#"),
      label
    )
