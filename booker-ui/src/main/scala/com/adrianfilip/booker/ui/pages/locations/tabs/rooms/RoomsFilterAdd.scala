package com.adrianfilip.booker.ui.pages.locations.tabs.rooms

import com.adrianfilip.booker.domain.model.{Building, Floor}
import com.adrianfilip.booker.scaleaware.building.GetAllBuildingsResponse
import com.adrianfilip.booker.scaleaware.room.{AddRoomRequest, AddRoomResponse, GetFilteredRoomsRequest}
import com.adrianfilip.booker.ui.domain.Event.LocationsPageEvents.RoomsEvents
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.domain.components.Bindings.{bindInputToVar, bindOnChangeToVar}
import com.adrianfilip.booker.ui.domain.components.Filter.floatingDivCSS
import com.adrianfilip.booker.ui.domain.components.Notifications
import com.adrianfilip.booker.ui.domain.components.NotificationsDiv.{
  RemoveAllNotifications,
  notificationDivs,
  removeAllNotifications
}
import com.adrianfilip.booker.ui.domain.model.{LoggedUserDetails, Notification}
import com.adrianfilip.booker.ui.services.{BuildingsService, RoomsService, SecurityErrors, ServiceErrors}
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.thisEvents
import zio.ZIO

import scala.util.{Failure, Success, Try}

case class RoomsFilterAdd(
  filters: Var[GetFilteredRoomsRequest],
  roomsService: RoomsService,
  roomsEventsStream: EventStream[RoomsEvents | SecurityEvent],
  roomsEventsObserver: Observer[RoomsEvents | SecurityEvent],
  buildingsService: BuildingsService,
  availableBuildings: Var[List[Building]],
  userDetails: LoggedUserDetails
)(implicit val runtime: zio.Runtime[Any]):

  private val codeNotifications: Var[List[Notification]]     = Var(List.empty)
  private val seatsNotifications: Var[List[Notification]]    = Var(List.empty)
  private val floorsNotifications: Var[List[Notification]]   = Var(List.empty)
  private val buildingNotifications: Var[List[Notification]] = Var(List.empty)
  private val addRoomNotifications: Var[List[Notification]]  = Var(List.empty)

  private val floors: Var[List[Floor]] = Var(Floor.values.toList)

  private val componentEventBus
    : EventBus[GetAllBuildingsResponse | AddRoomInvalid | AddRoomResponse | ServiceErrors | SecurityErrors] =
    new EventBus()

  private val componentEventsHandler =
    List(
      componentEventBus.events.collect {
        case a: SecurityErrors => {
          println("Security error")
          SecurityEvent(a)
        }
      } --> roomsEventsObserver,
      componentEventBus.events.collect {
        case a: ServiceErrors => {
          println("Service error")
          List(Notification.Error(s"${a.code} ${a.reason.map(": " + _).getOrElse("")}"))
        }
      } --> addRoomNotifications,
      componentEventBus.events.collect { case GetAllBuildingsResponse(buildings) =>
        buildings
      } --> availableBuildings,
      componentEventBus.events
        .collect { case a: AddRoomResponse.AddRoomResponseSuccess => a } --> { _ =>
        addRoomNotifications.removeAll()
        roomsEventsObserver.onNext(RoomsEvents.RefreshRoomsWithFilters)
      },
      componentEventBus.events
        .collect { case AddRoomResponse.AddRoomResponseFailure(code, messageOp) =>
          List(Notification.Error(s"$code ${messageOp.map(m => s": $m").getOrElse("")}"))
        } --> addRoomNotifications,
      componentEventBus.events
        .collect { case a: AddRoomInvalid => a } --> { a =>
        codeNotifications.set(a.codeNotifications)
        seatsNotifications.set(a.seatsNotifications)
        floorsNotifications.set(a.floorsNotifications)
        buildingNotifications.set(a.buildingNotifications)
      }
    )

  private val marginLeftDefault = 1
  private val codeWidth         = 10
  private val nameWidth         = 24
  private val floorWidth        = 13
  private val seatsWidth        = 9
  private val buildingWidth     = 29
  def make                      =
    div(
      div(
        floatingDivCSS(_width = codeWidth, _marginLeft = marginLeftDefault),
        className("form-group"),
        input(
          placeholder("Code"),
          className("form-control"),
          width("100%"),
          bindInputToVar[GetFilteredRoomsRequest](
            associatedVar = filters,
            inputToModel = {
              new PartialFunction[String, GetFilteredRoomsRequest]:
                override def isDefinedAt(x: String): Boolean           = true
                override def apply(s: String): GetFilteredRoomsRequest =
                  filters.now().copy(code = if (s.isEmpty) None else Some(s))
            },
            modelToInput = {
              new PartialFunction[GetFilteredRoomsRequest, String]:
                override def isDefinedAt(x: GetFilteredRoomsRequest): Boolean = true
                override def apply(m: GetFilteredRoomsRequest): String        = m.code.getOrElse("")
            },
            debounceValue = 200
          ),
          Notifications.cleanNotificationsOnEmptyInput(codeNotifications)
        ),
        notificationDivs(codeNotifications)
      ),
      div(
        floatingDivCSS(_width = nameWidth, _marginLeft = marginLeftDefault),
        className("form-group"),
        input(
          className("form-control"),
          placeholder("Name"),
          width("100%"),
          bindInputToVar[GetFilteredRoomsRequest](
            associatedVar = filters,
            inputToModel = {
              new PartialFunction[String, GetFilteredRoomsRequest]:
                override def isDefinedAt(x: String): Boolean           = true
                override def apply(s: String): GetFilteredRoomsRequest =
                  filters.now().copy(name = if (s.isEmpty) None else Some(s))
            },
            modelToInput = {
              new PartialFunction[GetFilteredRoomsRequest, String]:
                override def isDefinedAt(x: GetFilteredRoomsRequest): Boolean = true

                override def apply(m: GetFilteredRoomsRequest): String = m.name.getOrElse("")
            },
            debounceValue = 200
          )
        )
      ),
      div(
        floatingDivCSS(_width = floorWidth, _marginLeft = marginLeftDefault),
        select(
          width("100%"),
          children <-- floors.toObservable.map(floors =>
            List(option(value(""), "Floor..")) ++ floors.map(f => option(value(f.toString), f.toString))
          ),
          bindOnChangeToVar[GetFilteredRoomsRequest](
            associatedVar = filters,
            debounceValue = 0,
            modelToInput = {
              new PartialFunction[GetFilteredRoomsRequest, String]:
                override def isDefinedAt(x: GetFilteredRoomsRequest): Boolean = true
                override def apply(s: GetFilteredRoomsRequest): String        = s.floor.map(_.toString).getOrElse("")
            },
            inputToModel = {
              new PartialFunction[String, GetFilteredRoomsRequest]:
                override def isDefinedAt(s: String): Boolean           = s.isEmpty || Try(Floor.valueOf(s)).isSuccess
                override def apply(s: String): GetFilteredRoomsRequest =
                  filters.now().copy(floor = if (s.isEmpty) None else Try(Floor.valueOf(s)).toOption)
            }
          ),
          onChange.mapToValue --> { v => if (!v.isEmpty) floorsNotifications.set(List.empty) else () }
        ),
        notificationDivs(floorsNotifications)
      ),
      div(
        floatingDivCSS(_width = seatsWidth, _marginLeft = marginLeftDefault),
        className("form-group"),
        input(
          width("100%"),
          className("form-control"),
          placeholder("Seats"),
          onInput.mapToValue --> { s =>
            if (s.nonEmpty) {
              Try(s.toInt) match {
                case Failure(_) =>
                  seatsNotifications.set(List(Notification.Error("Invalid seats value. Must be number!")))
                case Success(_) => seatsNotifications.set(List.empty)
              }
            } else {
              seatsNotifications.set(List.empty)
            }
          },
          bindInputToVar[GetFilteredRoomsRequest](
            associatedVar = filters,
            inputToModel = {
              new PartialFunction[String, GetFilteredRoomsRequest]:
                override def isDefinedAt(s: String): Boolean           = (s.nonEmpty && Try(s.toInt).isSuccess) || s.isEmpty
                override def apply(s: String): GetFilteredRoomsRequest =
                  filters
                    .now()
                    .copy(seats = if (s.isEmpty) None else Some(s.toInt))
            },
            modelToInput = {
              new PartialFunction[GetFilteredRoomsRequest, String]:
                override def isDefinedAt(x: GetFilteredRoomsRequest): Boolean = true

                override def apply(m: GetFilteredRoomsRequest): String = m.seats.map(_.toString).getOrElse("")
            },
            debounceValue = 200
          )
        ),
        notificationDivs(seatsNotifications)
      ),
      div(
        floatingDivCSS(_width = buildingWidth, _marginLeft = marginLeftDefault),
        refreshBuildingsSubscription(buildingsService),
        onMountCallback(_ => roomsEventsObserver.onNext(RoomsEvents.RefreshBuildings)),
        select(
          value <-- filters.toObservable.map(rf => rf.buildingId.getOrElse("")),
          children <-- availableBuildings.toObservable.map(bs =>
            List(option(value(""), "Building..")) ++ bs.map(b =>
              option(value(b.uuid), s"${b.code} ${b.name.map(v => s" - $v").getOrElse("")}")
            )
          ),
          onChange.mapToValue --> { s =>
            filters.set(filters.now().copy(buildingId = if (s.isEmpty) None else Some(s)))
          },
          onChange.mapToValue --> { s =>
            if (!s.isEmpty) buildingNotifications.set(List.empty) else ()
          }
        ),
        notificationDivs(buildingNotifications)
      ),
      div(
        floatingDivCSS(9, 1),
        button(
          "Add room",
          className := "btn btn-primary",
          width("100%"),
          thisEvents(onClick).flatMap { _ => addRoom } --> componentEventBus
        ),
        button(
          "Clear filters",
          className := "btn btn-secondary",
          width("100%"),
          marginTop("20%"),
          onClick --> { _ =>
            cleanFilters(
              filters,
              codeNotifications,
              addRoomNotifications,
              seatsNotifications,
              floorsNotifications,
              buildingNotifications
            )
          }
        )
      ),
      notificationDivs(addRoomNotifications),
      componentEventsHandler
    )

  case class AddRoomInvalid(
    codeNotifications: List[Notification],
    seatsNotifications: List[Notification],
    floorsNotifications: List[Notification],
    buildingNotifications: List[Notification]
  )

  private def addRoom: EventStream[AddRoomInvalid | AddRoomResponse | ServiceErrors | SecurityErrors] =
    removeAllNotifications(
      codeNotifications,
      seatsNotifications,
      floorsNotifications,
      buildingNotifications,
      addRoomNotifications
    )
    val form                     = filters.now()
    println(s"form: $form")
    val codeNotificationsAdd     = form.code.map(_ => List.empty).getOrElse(List(Notification.Error("Missing code")))
    val seatsNotificationsAdd    = form.seats.map(_ => List.empty).getOrElse(List(Notification.Error("Missing seats.")))
    val floorsNotificationsAdd   = form.floor.map(_ => List.empty).getOrElse(List(Notification.Error("Missing floor.")))
    val buildingNotificationsAdd =
      form.buildingId.map(_ => List.empty).getOrElse(List(Notification.Error("Missing building.")))

    if (
      codeNotificationsAdd.nonEmpty || seatsNotificationsAdd.nonEmpty || floorsNotificationsAdd.nonEmpty || buildingNotificationsAdd.nonEmpty
    ) {
      EventStream.fromValue(
        AddRoomInvalid(
          codeNotifications = codeNotificationsAdd,
          seatsNotifications = seatsNotificationsAdd,
          floorsNotifications = floorsNotificationsAdd,
          buildingNotifications = buildingNotificationsAdd
        )
      )
    } else {
      zio.Unsafe
        .unsafe {
          runtime.unsafe
            .run {
              roomsService
                .addRoom(
                  m = AddRoomRequest(
                    code = form.code.get,
                    name = form.name,
                    floor = form.floor.get,
                    buildingId = form.buildingId.get,
                    seats = form.seats.get
                  ),
                  token = userDetails.token
                )
            }
            .getOrThrowFiberFailure()
        }
    }

  private def refreshBuildingsSubscription(buildingsService: BuildingsService) =
    roomsEventsStream
      .collect { case a: RoomsEvents.RefreshBuildings.type => a }
      .flatMap { _ =>
        zio.Unsafe
          .unsafe {
            runtime.unsafe
              .run(
                buildingsService
                  .getAllBuildings(token = userDetails.token)
              )
              .getOrThrowFiberFailure()
          }
      } --> componentEventBus

  private def cleanFilters(
    filters: Var[GetFilteredRoomsRequest],
    codeNotifications: Var[List[Notification]],
    addRoomNotifications: Var[List[Notification]],
    seatsNotifications: Var[List[Notification]],
    floorsNotifications: Var[List[Notification]],
    buildingNotifications: Var[List[Notification]]
  ) =
    removeAllNotifications(
      codeNotifications,
      addRoomNotifications,
      seatsNotifications,
      floorsNotifications,
      buildingNotifications
    )
    filters.set(GetFilteredRoomsRequest(code = None, name = None, floor = None, seats = None, buildingId = None))
