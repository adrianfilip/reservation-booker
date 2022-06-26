package com.adrianfilip.booker.ui.pages.myreservations

import com.adrianfilip.booker.domain.model.{Building, Floor, Room}
import com.adrianfilip.booker.scaleaware.building.GetAllBuildingsResponse
import com.adrianfilip.booker.scaleaware.reservation.AddReservationResponse
import com.adrianfilip.booker.scaleaware.room.{GetFilteredRoomsRequest, GetFilteredRoomsResponse}
import com.adrianfilip.booker.ui.Root.reservationService
import com.adrianfilip.booker.ui.domain.Event
import com.adrianfilip.booker.ui.domain.Event.MyReservationsPageEvents.MakeReservationEvents
import com.adrianfilip.booker.ui.domain.Event.MyReservationsPageEvents.MakeReservationEvents.*
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.domain.components.Bindings.{bindInputToVarAndNotifications, bindOnChangeToVar}
import com.adrianfilip.booker.ui.domain.components.NotificationsDiv.{notificationDivs, removeAllNotifications}
import com.adrianfilip.booker.ui.domain.model.{LoggedUserDetails, Notification}
import com.adrianfilip.booker.ui.pages.myreservations.MakeReservation.*
import com.adrianfilip.booker.ui.services.{
  BuildingsService,
  ReservationService,
  RoomsService,
  SecurityErrors,
  ServiceErrors
}
import com.adrianfilip.booker.ui.styling.DisplayValue
import com.adrianfilip.components.datepicker.Datepicker
import com.adrianfilip.components.datepicker.Datepicker.Day
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.thisEvents
import zio.{Runtime, UIO, ZIO}

import java.time.{LocalDate, LocalTime}
import scala.util.Try

case class MakeReservation(
  buildingsService: BuildingsService,
  roomsService: RoomsService,
  reservationService: ReservationService,
  makeReservationEventStream: EventStream[MakeReservationEvents | SecurityEvent],
  makeReservationEventObserver: Observer[MakeReservationEvents | SecurityEvent],
  userDetails: LoggedUserDetails
)(implicit runtime: zio.Runtime[Any]) {

  private val componentEventBus: EventBus[
    GetAllBuildingsResponse | GetFilteredRoomsResponse | SelectedBuilding | SelectedRoom |
      MakeReservation.RefreshAddReservationForm.type | AddReservationResponse | SelectedFloor | ServiceErrors |
      SecurityErrors
  ] = new EventBus()

  val defaultAddReservationForm = AddReservationForm(
    seats = 120,
    date = {
      val now = LocalDate.now()
      Day(year = now.getYear, month = now.getMonthValue, day = now.getDayOfMonth)
    },
    duration = 4,
    interval = computeIntervals(4).head,
    building = None,
    floor = None,
    room = None
  )

  val availableBuildings: Var[List[Building]] = Var(List.empty)
  val availableRooms: Var[List[Room]]         = Var(List.empty)

  val selectableRooms: Var[List[Room]]   = Var(List.empty)
  val selectableFloors: Var[List[Floor]] = Var(List.empty)

  private val durations: Var[List[Int]] = Var(List(1, 2, 3, 4, 8))

  private val day: Var[Day]                     = Var(Day.from(LocalDate.now()))
  private val showModal: Var[DisplayValue]      = Var(DisplayValue.None)
  private val focusDateInput: EventBus[Boolean] = new EventBus()

  private val addReservationForm: Var[AddReservationForm] = Var(defaultAddReservationForm)

  private val seatsNotifications: Var[List[Notification]]     = Var(List.empty)
  private val buildingNotifications: Var[List[Notification]]  = Var(List.empty)
  private val floorNotifications: Var[List[Notification]]     = Var(List.empty)
  private val roomNotifications: Var[List[Notification]]      = Var(List.empty)
  private val componentNotifications: Var[List[Notification]] = Var(List.empty)

  private def resetAddReservationForm = ZIO.attempt {
    addReservationForm.set(defaultAddReservationForm)
    selectableRooms.set(List.empty)
    selectableFloors.set(List.empty)
  }

  private def refreshForm =
    removeSeatsNotifications *> removeBuildingRoomFloorNotifications *> resetAddReservationForm

  private def handleComponentEvents =
    List(
      componentEventBus.events.collect { case a: SecurityErrors => SecurityEvent(a) } --> makeReservationEventObserver,
      componentEventBus.events.collect { case a: ServiceErrors =>
        List(Notification.Error(s"${a.code}: ${a.reason.map(r => s": $r").getOrElse("")}"))
      } --> componentNotifications,
      componentEventBus.events.collect { case GetAllBuildingsResponse(buildings) => buildings } --> availableBuildings,
      componentEventBus.events.collect {
        case SelectedBuilding(buildingId, GetFilteredRoomsResponse.GetFilteredRoomsResponseSuccess(rooms)) =>
          (buildingId, rooms)
      } --> { case (buildingId, ls) =>
        val floors = ls.map(_.floor).distinct
        val rooms  = ls
        selectableFloors.set(floors)
        availableRooms.set(rooms)
        //if I set here the value read from availableRooms it will not be the one I just set
        selectableRooms.set(rooms)
        addReservationForm.set(addReservationForm.now().copy(building = Some(buildingId), floor = None, room = None))
        println(s"Available floors: $floors")
        println(s"Available rooms: $rooms")
      },
      componentEventBus.events.collect {
        case SelectedBuilding(buildingId, GetFilteredRoomsResponse.GetFilteredRoomsResponseFailure(code, messageOp)) =>
          List(Notification.Error(s"${code}: for buildingId=$buildingId ${messageOp.map(r => s": $r").getOrElse("")}"))
      } --> componentNotifications,
      componentEventBus.events.collect { case AddReservationResponse.AddReservationResponseFailure(code, messageOp) =>
        List(Notification.Error(s"${code}: ${messageOp.map(r => s": $r").getOrElse("")}"))
      } --> componentNotifications,
      componentEventBus.events.collect { case _: AddReservationResponse.AddReservationResponseSuccess =>
        componentNotifications.set(List.empty)
        Event.MyReservationsPageEvents.MakeReservationEvents.AddedReservation
      } --> makeReservationEventObserver,
      componentEventBus.events.collect { case SelectedBuilding(_, se: ServiceErrors) => se } --> componentEventBus,
      componentEventBus.events.collect { case SelectedBuilding(_, se: SecurityErrors) => se } --> componentEventBus,
      componentEventBus.events.collect { case MakeReservation.RefreshAddReservationForm => } --> { _ =>
        println(s"Refreshing the form ")
        componentNotifications.set(List.empty)
        zio.Unsafe.unsafe {
          runtime.unsafe
            .run(refreshForm)
            .getOrThrowFiberFailure()
        }
      },
      componentEventBus.events.collect { case SelectedFloor(floor) => floor } --> { floor =>
        println(s"selected floor: $floor")

        zio.Unsafe.unsafe {
          runtime.unsafe
            .run(ZIO.debug(s"Selected floor - $floor") *> removeBuildingRoomFloorNotifications *> ZIO.attempt {
              val rooms = floor match {
                case Some(f) => availableRooms.now().filter(_.floor == f)
                case None    => availableRooms.now()
              }
              selectableRooms.set(rooms)
              addReservationForm.set(
                addReservationForm
                  .now()
                  .copy(floor = floor, room = if (rooms.length == 1) Some(rooms.head.uuid) else None)
              )
            })
            .getOrThrowFiberFailure()
        }
      },
      componentEventBus.events.collect { case SelectedRoom(room) => room } --> { room =>
        zio.Unsafe.unsafe {
          runtime.unsafe
            .run(
              ZIO.debug(s"selected room: $room") *>
                removeBuildingRoomFloorNotifications *>
                ZIO.attempt {
                  println(availableRooms.now().find(room.contains).map(_.floor))
                  if (addReservationForm.now().floor.isEmpty) {
                    val floor = availableRooms.now().find(ar => room.contains(ar.uuid)).map(_.floor)
                    selectableRooms.set(availableRooms.now().filter(ar => floor.contains(ar.floor)))
                    addReservationForm.set(
                      addReservationForm
                        .now()
                        .copy(room = room, floor = floor)
                    )
                  } else {
                    addReservationForm.set(addReservationForm.now().copy(room = room))
                  }
                }
            )
            .getOrThrowFiberFailure()
        }
      }
    )

  private val reactToReservationFormChanges =
    List(
      makeReservationEventStream
        .collect { case Refresh => }
        .flatMap(_ =>
          zio.Unsafe.unsafe {
            runtime.unsafe
              .run(refreshForm *> buildingsService.getAllBuildings(userDetails.token))
              .getOrThrowFiberFailure()
          }
        ) --> componentEventBus
    )

  def make =
    val marginLeftAll = 1
    val seatsWidth    = 5
    val dateWidth     = 14
    val durationWidth = 7
    val intervalWidth = 14
    val buildingWidth = 25
    val floorWidth    = 9
    val roomWidth     = 14
    div(
      reactToReservationFormChanges,
      width("100%"),
      div(
        width("100%"),
        float("left"),
        label(width(s"$seatsWidth%"), marginLeft("1%"), "Seats"),
        label(width(s"$dateWidth%"), marginLeft("1%"), "Date"),
        label(
          width(s"$durationWidth%"),
          marginLeft(s"$marginLeftAll%"),
          "Duration"
        ), //will be a select populated with values 1, 2, 4, 8, will default to 2
        label(
          width(s"$intervalWidth%"),
          marginLeft(s"$marginLeftAll%"),
          "Interval"
        ), //will be a select populated based on selected duration with values from 8 to 22
        label(
          width(s"$buildingWidth%"),
          marginLeft(s"$marginLeftAll%"),
          "Building"
        ), //will be a select populated with all buildings if no seats is set, otherwise only with buildings that have rooms with >= seats
        label(
          width(s"$floorWidth%"),
          marginLeft(s"$marginLeftAll%"),
          "Floor"
        ), //will be a select populated based on selected Building,
        label(
          width(s"$roomWidth%"),
          marginLeft(s"$marginLeftAll%"),
          "Room"
        )  //will be a select populated based on selected Building,
      ),
      div(
        float("left"),
        width("100%"),
        marginBottom("1%"),
        div(
          float("left"),
          width(s"$seatsWidth%"),
          marginLeft(s"$marginLeftAll%"),
          className("form-group"),
          input(
            width("100%"),
            className("form-control"),
            placeholder("Seats"),
            bindInputToVarAndNotifications[AddReservationForm](
              associatedVar = addReservationForm,
              associatedNotificationsVars = seatsNotifications,
              inputToModel = {
                case s if s.toIntOption.isDefined => addReservationForm.now().copy(seats = s.toInt)
              },
              modelToInput = _.seats.toString,
              inputToNotifications = Some({ case s =>
                s.toIntOption
                  .map(nr => if (nr <= 0) List(Notification.Error(s"'$s' is not greater than 0")) else List.empty)
                  .getOrElse(List(Notification.Error(s"'$s' is not a number! ")))
              }),
              debounceValue = 0
            ),
            //only necessary here because if there is an error in the input, the model is not updated
            value <-- componentEventBus.events.collect { case RefreshAddReservationForm =>
              defaultAddReservationForm.seats.toString
            }
          ),
          notificationDivs(seatsNotifications)
        ),
        div(
          float("left"),
          width(s"$dateWidth%"),
          marginLeft(s"$marginLeftAll%"),
          Datepicker(day, showModal).makeModal,
          className("form-group"),
          input(
            width("100%"),
            className("form-control"),
            focus <-- focusDateInput,
            onClick.map(_ => DisplayValue.Block) --> showModal,
            onClick.map(_ => false) --> focusDateInput,
            value <-- day.signal.map(prettyPrintDay),
            day --> addReservationForm.toObserver.contramap[Day](day => addReservationForm.now().copy(date = day)),
            addReservationForm --> day.toObserver.contramap[AddReservationForm](form => form.date)
          )
        ),
        div(
          width(s"$durationWidth%"),
          marginLeft(s"$marginLeftAll%"),
          float("left"),
          //always shows options
          select(
            width("100%"),
            children <-- durations.toObservable.map(durations =>
              durations.map(d => option(value(d.toString), s"$d h"))
            ),
            bindOnChangeToVar[AddReservationForm](
              associatedVar = addReservationForm,
              inputToModel = {
                case s if s.toIntOption.isDefined =>
                  addReservationForm.now().copy(duration = s.toIntOption.get, interval = computeIntervals(s.toInt).head)
              },
              modelToInput = m => m.duration.toString,
              debounceValue = 0
            )
          )
        ),
        div(
          width(s"$intervalWidth%"),
          marginLeft(s"$marginLeftAll%"),
          float("left"),
          //show intervals options based on duration, only that influences it
          select(
            width("100%"),
            placeholder("Interval"),
            bindOnChangeToVar[AddReservationForm](
              associatedVar = addReservationForm,
              inputToModel = {
                case s if Interval.parse(s).isDefined => addReservationForm.now().copy(interval = Interval.parse(s).get)
              },
              modelToInput = m => Interval.toPrettyString(m.interval),
              debounceValue = 0
            ),
            children <-- addReservationForm.signal
              .map(_.duration)
              .map(duration =>
                computeIntervals(duration)
                  .map(i => option(value(Interval.toPrettyString(i)), Interval.toPrettyString(i)))
              )
          )
        ),
        div(
          width(s"$buildingWidth%"),
          marginLeft(s"$marginLeftAll%"),
          float("left"),
          //all fields starting from this one are computed/recomputed based on previous fields
          //here I only show buildings that have rooms available matching the parameters above
          select(
            width("100%"),
            value <-- addReservationForm.signal.map(_.building.getOrElse("")),
            thisEvents(onChange)
              .map(_.target.asInstanceOf[org.scalajs.dom.HTMLSelectElement].value)
              .flatMap { buildingId =>
                if (buildingId.isEmpty) {
                  selectableFloors.set(List.empty)
                  availableRooms.set(List.empty)
                  selectableRooms.set(List.empty)
                  addReservationForm.set(addReservationForm.now().copy(building = None, floor = None, room = None))
                  EventStream.empty
                } else {
                  buildingNotifications.set(List.empty)
                  zio.Unsafe
                    .unsafe {
                      runtime.unsafe
                        .run(
                          roomsService
                            .getFilteredRooms(
                              m = GetFilteredRoomsRequest(
                                code = None,
                                name = None,
                                floor = None,
                                seats = None,
                                buildingId = Some(buildingId)
                              ),
                              token = userDetails.token
                            )
                        )
                        .getOrThrowFiberFailure()
                    }
                    .map(SelectedBuilding(buildingId, _))
                }
              } --> componentEventBus,
            children <-- availableBuildings.toObservable.map(buildings =>
              List(option(value(""), "All buildings")) ++ buildings.map(b =>
                option(value(b.uuid), b.code + b.name.map(" - " + _).getOrElse(""))
              )
            )
          ),
          notificationDivs(buildingNotifications)
        ),
        div(
          width(s"$floorWidth%"),
          marginLeft(s"$marginLeftAll%"),
          float("left"),
          //if a building is selected then only floors from that (that also fit the rest of filters) are displayed
          //otherwise all floors is the only option and the field is uneditable
          select(
            width("100%"),
            value <-- addReservationForm.signal.map(_.floor.map(_.toString).getOrElse("")),
            children <-- selectableFloors.toObservable.map(floors =>
              List(option(value(""), "All floors")) ++ floors.map(b => option(value(b.toString), b.toString))
            ),
            onChange.mapToValue --> componentEventBus.writer.contramap[String](floor =>
              SelectedFloor(Try(Floor.valueOf(floor)).toOption)
            )
          ),
          notificationDivs(floorNotifications)
        ),
        div(
          width(s"$roomWidth%"),
          marginLeft(s"$marginLeftAll%"),
          float("left"),
          // If building is selected then only rooms from that (that also fit the rest of filters) are displayed
          // otherwise all rooms from all buildings that meet the filters are displayed
          select(
            width("100%"),
            value <-- addReservationForm.signal.map(_.room.getOrElse("")),
            children <-- selectableRooms.toObservable.map(rooms =>
              List(option(value(""), "All rooms")) ++ rooms.map(b =>
                option(value(b.uuid), b.code + b.name.map(n => s" - $n").getOrElse(""))
              )
            ),
            onChange.mapToValue --> componentEventBus.writer.contramap[String](room =>
              SelectedRoom(roomId = if (room.isEmpty) None else Some(room))
            )
          ),
          notificationDivs(roomNotifications)
        )
      ),
      div(
        float("left"),
        width("100%"),
        marginLeft(s"$marginLeftAll%"),
        button("Make reservation", className := "btn btn-primary", clickMakeReservation),
        button(
          "Cancel",
          className                          := "btn btn-secondary",
          marginLeft("5%"),
          thisEvents(onClick).map(_ => RefreshAddReservationForm) --> componentEventBus
        )
      ),
      notificationDivs(componentNotifications),
      handleComponentEvents
    )

  private def clickMakeReservation =
    thisEvents(onClick)
      .flatMap { _ =>
        val reservationForm = addReservationForm.now()

        if (reservationForm.building.isEmpty) {
          buildingNotifications.set(List(Notification.Error("Building is required")))
        }
        if (reservationForm.room.isEmpty) {
          roomNotifications.set(List(Notification.Error("Room is required")))
        }
        if (reservationForm.floor.isEmpty) {
          floorNotifications.set(List(Notification.Error("Floor is required")))
        }

        if (reservationForm.building.isEmpty || reservationForm.room.isEmpty || reservationForm.floor.isEmpty) {
          EventStream.empty
        } else {
          zio.Unsafe.unsafe {
            runtime.unsafe
              .run(
                reservationService.addReservation(
                  m = ReservationService.AddReservation(
                    seats = reservationForm.seats,
                    date = reservationForm.date,
                    duration = reservationForm.duration,
                    interval = reservationForm.interval,
                    buildingId = reservationForm.building.get,
                    floor = reservationForm.floor.get,
                    roomId = reservationForm.room.get,
                    holderId = userDetails.user.username
                  ),
                  token = userDetails.token
                )
              )
              .getOrThrowFiberFailure()
          }
        }
      } --> componentEventBus

  private def prettyPrintDay(d: Day) =
    s"${padDateValue(d.day)}-${padDateValue(d.month)}-${d.year}"

  private def padDateValue(d: Int) =
    if (d <= 9) s"0$d" else s"$d"

  case class MakeReservationFilter(seats: Int, buildingId: Long, duration: Int)

  def computeIntervals(duration: Int): List[Interval] =
    (8 to (22 - duration)).toList
      .map(t => LocalTime.of(t, 0))
      .map(t => Interval(t, t.plusHours(duration.toLong)))

  private def removeBuildingNotifications = ZIO.attempt {
    removeAllNotifications(buildingNotifications)
  }
  private def removeFloorsNotifications   = ZIO.attempt {
    removeAllNotifications(floorNotifications)
  }

  private def removeRoomNotifications = ZIO.attempt {
    removeAllNotifications(roomNotifications)
  }

  private def removeBuildingRoomFloorNotifications =
    removeBuildingNotifications *> removeFloorsNotifications *> removeRoomNotifications

  private def removeSeatsNotifications = ZIO.attempt {
    removeAllNotifications(seatsNotifications)
  }

}

object MakeReservation {
  case class SelectedBuilding(buildingId: String, e: GetFilteredRoomsResponse | ServiceErrors | SecurityErrors)
  object RefreshAddReservationForm
  case class AddReservation(form: AddReservationForm)
  case class MakeReservationInputValidation(
    buildingNotifications: List[Notification],
    roomNotifications: List[Notification],
    floorNotifications: List[Notification]
  )
  case class SelectedFloor(floor: Option[Floor])
  case class SelectedRoom(roomId: Option[String])

  case class AddReservationForm(
    seats: Int,
    date: Day,
    duration: Int,
    interval: Interval,
    building: Option[String],
    floor: Option[Floor],
    room: Option[String]
  )

  case class Interval(start: LocalTime, end: LocalTime)

  object Interval {
    def toPrettyString(interval: Interval): String = {
      val start = interval.start.toString
      val end   = interval.end.toString
      s"$start-$end"
    }

    def parse(prettyString: String): Option[Interval] =
      Try {
        val split = prettyString.split("-")
        val start = LocalTime.parse(split(0))
        val end   = LocalTime.parse(split(1))
        Interval(start, end)
      }.toOption
  }
}
