package com.adrianfilip.booker.ui.pages.locations.tabs.rooms

import com.adrianfilip.booker.domain.model.{Building, Floor, Room}
import com.adrianfilip.booker.scaleaware.room.{
  GetFilteredRoomsRequest,
  GetFilteredRoomsResponse,
  UpdateRoomRequest,
  UpdateRoomResponse
}
import com.adrianfilip.booker.ui.domain.Event.LocationsPageEvents.RoomsEvents
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.domain.components.Bindings.bindOnChangeToVar
import com.adrianfilip.booker.ui.domain.components.NotificationsDiv.{notificationDivs, removeAllNotifications}
import com.adrianfilip.booker.ui.domain.model.{LoggedUserDetails, Notification}
import com.adrianfilip.booker.ui.services.{RoomsService, SecurityErrors, ServiceErrors}
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.thisEvents
import zio.{Runtime, ZIO}

import scala.util.{Failure, Success, Try}

case class RoomsTable(
  roomsService: RoomsService,
  roomsEventsObserver: Observer[RoomsEvents | SecurityEvent],
  roomsEventsStream: EventStream[RoomsEvents | SecurityEvent],
  filters: Var[GetFilteredRoomsRequest],
  availableBuildings: Var[List[Building]],
  userDetails: LoggedUserDetails
):

  private val runtime                   = Runtime.default
  private val allRooms: Var[List[Room]] = Var[List[Room]](List.empty)

  private val selectedRoomToEdit: Var[Option[SelectedRoomEditForm]] = Var(None)

  private def editingRoom(room: Room): Signal[Boolean] =
    selectedRoomToEdit.toObservable.map(_.map(_.uuid).contains(room.uuid))

  private def showWhenEditing(room: Room) = hidden <-- editingRoom(room).map(!_)
  private def hideWhenEditing(room: Room) = hidden <-- editingRoom(room)

  private val componentNotifications: Var[List[Notification]] = Var(List.empty)
  private val editCodeNotifications: Var[List[Notification]]  = Var(List.empty)
  private val editSeatsNotifications: Var[List[Notification]] = Var(List.empty)
  private val editRoomNotifications: Var[List[Notification]]  = Var(List.empty)

  private val floors: Var[List[Floor]] = Var(Floor.values.toList)

  case class SelectedRoomEditForm(
    uuid: String,
    code: String,
    seats: Int,
    name: Option[String],
    floor: Floor,
    buildingId: String
  )

  private val componentEventBus: EventBus[
    GetFilteredRoomsResponse | UpdateRoomResponse | RoomsTable.InvalidUpdateBuildingInputs | ServiceErrors |
      SecurityErrors
  ] =
    new EventBus()

  private def pageEventsHandler =
    List(
      roomsEventsStream
        .collect { case a: RoomsEvents.RefreshRoomsWithFilters.type => a }
        .flatMap { _ =>
          zio.Unsafe
            .unsafe {
              runtime.unsafe
                .run(roomsService.getFilteredRooms(filters.now(), userDetails.token))
                .getOrThrowFiberFailure()
            }
        } --> componentEventBus
    )

  private def componentEventsHandler =
    List(
      componentEventBus.events
        .collect { case a: GetFilteredRoomsResponse.GetFilteredRoomsResponseSuccess => a.rooms } --> allRooms,
      componentEventBus.events
        .collect { case a: GetFilteredRoomsResponse.GetFilteredRoomsResponseFailure =>
          List(Notification.Error(s"${a.code}: ${a.message.map(s => s": $s")}"))
        } --> componentNotifications,
      componentEventBus.events
        .collect { case a: UpdateRoomResponse.UpdateRoomResponseSuccess => a } --> { _ =>
        editingEndedCleanup()
        roomsEventsObserver.onNext(RoomsEvents.RefreshRoomsWithFilters)
      },
      componentEventBus.events
        .collect { case a: UpdateRoomResponse.UpdateRoomResponseFailure =>
          List(Notification.Error(s"${a.code}: ${a.message.map(s => s": $s")}"))
        } --> editRoomNotifications,
      componentEventBus.events
        .collect { case a: SecurityErrors =>
          SecurityEvent(a)
        } --> roomsEventsObserver,
      componentEventBus.events
        .collect { case a: ServiceErrors =>
          List(Notification.Error(s"${a.code}: ${a.reason.map(r => s": $r").getOrElse("")}"))
        } --> componentNotifications
    )

  private val codeColumn     = 11
  private val nameColumn     = 25
  private val floorColumn    = 14
  private val seatsColumn    = 10
  private val buildingColumn = 30
  private val lastColumn     = 10
  def make                   =
    div(
      width := "100%",
      notificationDivs(componentNotifications),
      table(
        className("table"),
        thead(
          className("thead"),
          th("Room code", width(s"$codeColumn%")),
          th("Room name", width(s"$nameColumn%")),
          th("Room floor", width(s"$floorColumn%")),
          th("Seats", width(s"$seatsColumn%")),
          th("Building", width(s"$buildingColumn%")),
          th("", width(s"$lastColumn%"))
        ),
        tbody(
          filters.toObservable --> { _ => roomsEventsObserver.onNext(RoomsEvents.RefreshRoomsWithFilters) },
          children <-- allRooms.toObservable
            .map(ls =>
              ls.flatMap(room =>
                List(
                  tr(
                    width("100%"),
                    hideWhenEditing(room),
                    td(room.code, width(s"$codeColumn%")),
                    td(room.name, width(s"$nameColumn%")),
                    td(room.floor.toString, width(s"$floorColumn%")),
                    td(room.seats, width(s"$seatsColumn%")),
                    td(
                      room.building.code + room.building.name.map(" (" + _ + ")").getOrElse(""),
                      width(s"$buildingColumn%")
                    ),
                    td(width(s"$lastColumn%"), div(float("left"), editRoomButton(room)))
                  ),
                  tr(
                    width("100%"),
                    showWhenEditing(room),
                    td(
                      width(s"$codeColumn%"),
                      className("form-group"),
                      input(
                        width("100%"),
                        className("form-control"),
                        value <-- selectedRoomToEdit.toObservable.map(_.map(_.code).getOrElse("")),
                        onInput.mapToValue
                          .map(v => selectedRoomToEdit.now().map(_.copy(code = v))) --> selectedRoomToEdit,
                        onInput.mapToValue
                          .map(_.isEmpty)
                          .filter(_ == false)
                          .map(_ => List.empty[Notification]) --> editRoomNotifications.toObserver
                      ),
                      notificationDivs(editCodeNotifications, editRoomNotifications)
                    ),
                    td(
                      width(s"$nameColumn%"),
                      className("form-group"),
                      input(
                        width("100%"),
                        className("form-control"),
                        value <-- selectedRoomToEdit.toObservable.map(_.flatMap(_.name).getOrElse("")),
                        onInput.mapToValue
                          .map(v => selectedRoomToEdit.now().map(_.copy(name = Some(v)))) --> selectedRoomToEdit
                      )
                    ),
                    td(
                      width(s"$floorColumn%"),
                      select(
                        width("100%"),
                        bindOnChangeToVar[Option[SelectedRoomEditForm]](
                          associatedVar = selectedRoomToEdit,
                          inputToModel = {
                            case s if Try(Floor.valueOf(s)).isSuccess =>
                              selectedRoomToEdit.now().map(_.copy(floor = Try(Floor.valueOf(s)).toOption.get))
                          },
                          modelToInput = m => m.map(_.floor).map(_.toString).getOrElse("")
                        ),
                        children <-- floors.toObservable.map(floors =>
                          List(option(value(""), "Floor..")) ++ floors.map(f => option(value(f.toString), f.toString))
                        )
                      )
                    ),
                    td(
                      width(s"$seatsColumn%"),
                      className("form-group"),
                      input(
                        width("100%"),
                        className("form-control"),
                        value <-- selectedRoomToEdit.toObservable.map(_.map(_.seats.toString).getOrElse("")),
                        onInput.mapToValue
                          .map(v =>
                            selectedRoomToEdit
                              .now()
                              .map(_.copy(seats = Try(v.toInt) match {
                                case Failure(_)     => {
                                  editSeatsNotifications.set(List(Notification.Error("Seats value must be a number!")))
                                  selectedRoomToEdit.now().map(_.seats).get
                                }
                                case Success(value) => {
                                  editSeatsNotifications.set(List.empty)
                                  value
                                }
                              }))
                          ) --> selectedRoomToEdit
                      ),
                      notificationDivs(editSeatsNotifications)
                    ),
                    td(
                      width(s"$buildingColumn%"),
                      select(
                        value <-- selectedRoomToEdit.toObservable.map(_.map(_.buildingId).getOrElse("")),
                        children <-- availableBuildings.toObservable.map(bs =>
                          List(option(value(""), "Building..")) ++ bs.map(b =>
                            option(value(b.uuid), s"${b.code} - ${b.name.map(s => s"($s)").getOrElse("")}")
                          )
                        ),
                        onChange.mapToValue
                          .map(v => selectedRoomToEdit.now().map(_.copy(buildingId = v))) --> selectedRoomToEdit
                      )
                    ),
                    td(
                      width(s"$lastColumn%"),
                      div(
                        width("70%"),
                        float("left"),
                        button(
                          "Save",
                          className := "btn btn-primary",
                          onClickUpdateRoom(roomsService, selectedRoomToEdit)
                        )
                      ),
                      div(
                        width("10%"),
                        float("right"),
                        button("X", className := "btn btn-danger", onClick --> { _ => editingEndedCleanup() })
                      )
                    )
                  )
                )
              )
            ),
          componentEventsHandler,
          pageEventsHandler,
          onMountCallback(_ => roomsEventsObserver.onNext(RoomsEvents.RefreshRoomsWithFilters))
        )
      )
    )

  def editingEndedCleanup() = {
    removeAllNotifications(editCodeNotifications, editRoomNotifications)
    selectedRoomToEdit.set(None)
  }

  def editRoomButton(room: Room) = button(
    "Edit",
    className := "btn btn-warning",
    onClick --> { _ =>
      selectedRoomToEdit.set(
        Some(
          SelectedRoomEditForm(
            uuid = room.uuid,
            code = room.code,
            name = room.name,
            floor = room.floor,
            buildingId = room.building.uuid,
            seats = room.seats
          )
        )
      )
    }
  )

  def onClickUpdateRoom(roomsService: RoomsService, selectedRoomEditForm: Var[Option[SelectedRoomEditForm]]) =
    thisEvents(onClick)
      .map(_ => selectedRoomEditForm.now())
      .collect { case Some(form) => form }
      .flatMap { form =>
        if (form.code.isEmpty) {
          EventStream.fromValue(
            RoomsTable.InvalidUpdateBuildingInputs(code = List(Notification.Error("Code is required!")))
          )
        } else {
          zio.Unsafe
            .unsafe {
              runtime.unsafe
                .run(
                  roomsService.updateRoom(
                    input = UpdateRoomRequest(
                      uuid = form.uuid,
                      code = form.code,
                      name = form.name,
                      floor = form.floor,
                      buildingId = form.buildingId,
                      seats = form.seats
                    ),
                    userDetails.token
                  )
                )
                .getOrThrowFiberFailure()
            }
        }
      } --> componentEventBus

object RoomsTable:
  case class InvalidUpdateBuildingInputs(code: List[Notification])
