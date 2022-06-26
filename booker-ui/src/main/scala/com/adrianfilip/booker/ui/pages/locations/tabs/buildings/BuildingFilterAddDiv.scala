package com.adrianfilip.booker.ui.pages.locations.tabs.buildings

import com.adrianfilip.booker.scaleaware.building.{AddBuildingRequest, AddBuildingResponse, GetFilteredBuildingsRequest}
import com.adrianfilip.booker.ui.domain.Event.LocationsPageEvents.BuildingsEvents
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.domain.components.Bindings.bindInputToVar
import com.adrianfilip.booker.ui.domain.components.Filter.floatingDivCSS
import com.adrianfilip.booker.ui.domain.components.Notifications
import com.adrianfilip.booker.ui.domain.components.NotificationsDiv.{
  RemoveAllNotifications,
  notificationDivs,
  removeAllNotifications
}
import com.adrianfilip.booker.ui.domain.model.{LoggedUserDetails, Notification}
import com.adrianfilip.booker.ui.services.{BuildingsService, SecurityErrors, ServiceErrors}
import com.raquo.laminar.api.L.*
import zio.ZIO
import io.laminext.syntax.core.thisEvents

case class BuildingFilterAddDiv(
  filters: Var[GetFilteredBuildingsRequest],
  buildingsService: BuildingsService,
  observer: Observer[BuildingsEvents | SecurityEvent],
  userDetails: LoggedUserDetails
)(implicit val runtime: zio.Runtime[Any]):

  private val codeNotifications: Var[List[Notification]]        = Var(List.empty)
  private val addBuildingNotifications: Var[List[Notification]] = Var(List.empty)

  private val componentEventBus: EventBus[AddBuildingResponse | ServiceErrors | SecurityErrors | InvalidInputs] =
    EventBus()

  private val handleComponentEvents =
    List(
      componentEventBus.events.collect { case a: SecurityErrors => SecurityEvent(a) } --> observer,
      componentEventBus.events.collect { case a: ServiceErrors =>
        List(Notification.Error(s"${a.code} ${a.reason.map(m => s": $m")}"))
      } --> addBuildingNotifications,
      componentEventBus.events.collect { case a: AddBuildingResponse.AddBuildingResponseSuccess => a } --> { _ =>
        addBuildingNotifications.removeAll()
        observer.onNext(BuildingsEvents.RefreshBuildingsWithFilters)
      },
      componentEventBus.events.collect { case AddBuildingResponse.AddBuildingResponseFailure(code, message) =>
        List(Notification.Error(s"$code ${message.map(m => s": $m").getOrElse("")}"))
      } --> addBuildingNotifications,
      componentEventBus.events.collect { case InvalidInputs(codeNotifications) =>
        codeNotifications
      } --> codeNotifications
    )

  def make =
    div(
      div(
        floatingDivCSS(_width = 29, _marginLeft = 1),
        className("form-group"),
        input(
          placeholder("Code"),
          className("form-control"),
          width("100%"),
          bindInputToVar[GetFilteredBuildingsRequest](
            associatedVar = filters,
            inputToModel = {
              new PartialFunction[String, GetFilteredBuildingsRequest]:
                override def isDefinedAt(x: String): Boolean               = true
                override def apply(s: String): GetFilteredBuildingsRequest =
                  filters.now().copy(code = if (s.isEmpty) None else Some(s))
            },
            modelToInput = {
              new PartialFunction[GetFilteredBuildingsRequest, String]:
                override def isDefinedAt(x: GetFilteredBuildingsRequest): Boolean = true
                override def apply(m: GetFilteredBuildingsRequest): String        = m.code.getOrElse("")
            },
            debounceValue = 200
          ),
          Notifications.cleanNotificationsOnEmptyInput(codeNotifications)
        ),
        notificationDivs(codeNotifications, addBuildingNotifications)
      ),
      div(
        floatingDivCSS(_width = 29, _marginLeft = 1),
        className("form-group"),
        textArea(
          placeholder("Name"),
          className("form-control"),
          width("100%"),
          bindInputToVar[GetFilteredBuildingsRequest](
            associatedVar = filters,
            inputToModel = {
              new PartialFunction[String, GetFilteredBuildingsRequest]:
                override def isDefinedAt(x: String): Boolean = true

                override def apply(s: String): GetFilteredBuildingsRequest = filters.now().copy(name = Some(s))
            },
            modelToInput = {
              new PartialFunction[GetFilteredBuildingsRequest, String]:
                override def isDefinedAt(x: GetFilteredBuildingsRequest): Boolean = true
                override def apply(fs: GetFilteredBuildingsRequest): String       = fs.name.getOrElse("")
            },
            debounceValue = 200
          )
        )
      ),
      div(
        floatingDivCSS(_width = 29, _marginLeft = 1),
        className("form-group"),
        textArea(
          placeholder("Address"),
          className("form-control"),
          width("100%"),
          bindInputToVar[GetFilteredBuildingsRequest](
            associatedVar = filters,
            inputToModel = {
              new PartialFunction[String, GetFilteredBuildingsRequest]:
                override def isDefinedAt(x: String): Boolean               = true
                override def apply(s: String): GetFilteredBuildingsRequest = filters.now().copy(address = Some(s))
            },
            modelToInput = {
              new PartialFunction[GetFilteredBuildingsRequest, String]:
                override def isDefinedAt(x: GetFilteredBuildingsRequest): Boolean = true
                override def apply(fs: GetFilteredBuildingsRequest): String       = fs.address.getOrElse("")
            },
            debounceValue = 200
          )
        )
      ),
      div(
        floatingDivCSS(9, 1),
        button(
          "Add building",
          className := "btn btn-primary",
          width("100%"),
          onClickAddBuilding(filters = filters, buildingsService = buildingsService)
        ),
        button(
          "Clear filters",
          className := "btn btn-secondary",
          width("100%"),
          marginTop("20%"),
          onClick --> { _ =>
            cleanFilters(filters, codeNotifications, addBuildingNotifications)
          }
        )
      ),
      handleComponentEvents
    )

  private def onClickAddBuilding(filters: Var[GetFilteredBuildingsRequest], buildingsService: BuildingsService) =
    thisEvents(onClick).flatMap { _ =>
      val form = filters.now()
      if (form.code.isEmpty) {
        EventStream.fromValue(InvalidInputs(codeNotifications = List(Notification.Error("Missing code"))))
      } else {
        zio.Unsafe
          .unsafe {
            runtime.unsafe
              .run(
                buildingsService
                  .addBuilding(
                    AddBuildingRequest(code = form.code.get, name = form.name, address = form.address),
                    token = userDetails.token
                  )
              )
              .getOrThrowFiberFailure()
          }
      }
    } --> componentEventBus

  case class InvalidInputs(codeNotifications: List[Notification])

  private def cleanFilters(
    filters: Var[GetFilteredBuildingsRequest],
    codeNotifications: Var[List[Notification]],
    addBuildingNotifications: Var[List[Notification]]
  ) =
    removeAllNotifications(codeNotifications, addBuildingNotifications)
    filters.set(GetFilteredBuildingsRequest(code = None, name = None, address = None))
