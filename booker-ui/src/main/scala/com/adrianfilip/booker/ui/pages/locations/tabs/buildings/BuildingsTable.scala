package com.adrianfilip.booker.ui.pages.locations.tabs.buildings

import com.adrianfilip.booker.domain.model.Building
import com.adrianfilip.booker.scaleaware.building.{
  GetFilteredBuildingsRequest,
  GetFilteredBuildingsResponse,
  UpdateBuildingRequest,
  UpdateBuildingResponse
}
import com.adrianfilip.booker.ui.domain.Event.LocationsPageEvents.BuildingsEvents
import com.adrianfilip.booker.ui.domain.Event.SecurityEvent
import com.adrianfilip.booker.ui.domain.components.NotificationsDiv.{notificationDivs, removeAllNotifications}
import com.adrianfilip.booker.ui.domain.model.{LoggedUserDetails, Notification}
import com.adrianfilip.booker.ui.services.{BuildingsService, SecurityErrors, ServiceErrors}
import com.raquo.laminar.api.L.*
import io.laminext.syntax.core.thisEvents
import zio.*

case class BuildingsTable(
  buildingsService: BuildingsService,
  buildingsEventsObserver: Observer[BuildingsEvents | SecurityEvent],
  buildingsEventsStream: EventStream[BuildingsEvents | SecurityEvent],
  filters: Var[GetFilteredBuildingsRequest],
  userDetails: LoggedUserDetails
)(implicit runtime: Runtime[Any]):

  private val allBuildings: Var[List[Building]] = Var[List[Building]](List.empty)

  private val selectedBuildingToEdit: Var[Option[SelectedBuildingEditForm]] = Var(None)

  private def editingBuilding(building: Building): Signal[Boolean] =
    selectedBuildingToEdit.toObservable.map(_.map(_.uuid).contains(building.uuid))

  private def showWhenEditing(building: Building) = hidden <-- editingBuilding(building).map(!_)

  private def hideWhenEditing(building: Building) = hidden <-- editingBuilding(building)

  private val componentNotifications: Var[List[Notification]]    = Var(List.empty)
  private val editCodeNotifications: Var[List[Notification]]     = Var(List.empty)
  private val editBuildingNotifications: Var[List[Notification]] = Var(List.empty)

  case class SelectedBuildingEditForm(uuid: String, code: String, name: Option[String], address: Option[String])

  private val componentEventBus: EventBus[
    GetFilteredBuildingsResponse | UpdateBuildingResponse | BuildingsTable.InvalidEditBuildingInputs | ServiceErrors |
      SecurityErrors
  ] =
    new EventBus()

  private def pageEventsHandler =
    List(
      buildingsEventsStream
        .collect { case a: BuildingsEvents.RefreshBuildingsWithFilters.type => a }
        .flatMap { _ =>
          zio.Unsafe
            .unsafe {
              runtime.unsafe
                .run(buildingsService.getFilteredBuildings(filters.now(), userDetails.token))
                .getOrThrowFiberFailure()
            }
        } --> componentEventBus
    )

  private def componentEventsHandler =
    List(
      componentEventBus.events
        .collect { case GetFilteredBuildingsResponse(buildings) =>
          buildings
        } --> allBuildings,
      componentEventBus.events
        .collect { case a: SecurityErrors =>
          SecurityEvent(a)
        } --> buildingsEventsObserver,
      componentEventBus.events
        .collect { case a: ServiceErrors =>
          List(Notification.Error(s"${a.code}: ${a.reason.map(r => s": $r").getOrElse("")}"))
        } --> componentNotifications,
      componentEventBus.events
        .collect { case a: UpdateBuildingResponse.UpdateBuildingResponseSuccess => a } --> { _ =>
        editingEndedCleanup()
        componentNotifications.set(List.empty)
        editBuildingNotifications.set(List.empty)
        editCodeNotifications.set(List.empty)
        buildingsEventsObserver.onNext(BuildingsEvents.RefreshBuildingsWithFilters)
      },
      componentEventBus.events
        .collect { case a: UpdateBuildingResponse.UpdateBuildingResponseFailure => a } --> { a =>
        editBuildingNotifications.set(
          List(Notification.Error(s"${a.code}: ${a.message.map(r => s": $r").getOrElse("")}"))
        )
        buildingsEventsObserver.onNext(BuildingsEvents.RefreshBuildingsWithFilters)
      },
      componentEventBus.events
        .collect { case BuildingsTable.InvalidEditBuildingInputs(codeNotifications) =>
          codeNotifications
        } --> editCodeNotifications
    )

  def make =
    div(
      width("100%"),
      notificationDivs(componentNotifications),
      table(
        className("table"),
        thead(
          className("thead"),
          th("Building Code", width("30%")),
          th("Building Name", width("30%")),
          th("Building Address", width("30%")),
          th("", width("10%"))
        ),
        tbody(
          filters.toObservable --> { _ => buildingsEventsObserver.onNext(BuildingsEvents.RefreshBuildingsWithFilters) },
          children <-- allBuildings.toObservable
            .map(ls =>
              ls.flatMap(building =>
                List(
                  tr(
                    width("100%"),
                    hideWhenEditing(building),
                    td(building.code, width("30%")),
                    td(building.name, width("30%")),
                    td(building.address, width("25%")),
                    td(width("15%"), div(float("left"), editBuildingButton(building)))
                  ),
                  tr(
                    width("100%"),
                    showWhenEditing(building),
                    td(
                      width("30%"),
                      className("form-group"),
                      input(
                        width("100%"),
                        className("form-control"),
                        value <-- selectedBuildingToEdit.toObservable.map(_.map(_.code).getOrElse("")),
                        onInput.mapToValue
                          .map(v => selectedBuildingToEdit.now().map(_.copy(code = v))) --> selectedBuildingToEdit,
                        onInput.mapToValue.map(_.isEmpty).filter(_ == false).map { _ =>
                          List.empty[Notification]
                        } --> editCodeNotifications.toObserver
                      ),
                      notificationDivs(editCodeNotifications, editBuildingNotifications)
                    ),
                    td(
                      width("30%"),
                      className("form-group"),
                      textArea(
                        width("100%"),
                        className("form-control"),
                        value <-- selectedBuildingToEdit.toObservable.map(_.flatMap(_.name).getOrElse("")),
                        onInput.mapToValue
                          .map(v => selectedBuildingToEdit.now().map(_.copy(name = Some(v)))) --> selectedBuildingToEdit
                      )
                    ),
                    td(
                      width("25%"),
                      className("form-group"),
                      textArea(
                        width("100%"),
                        className("form-control"),
                        value <-- selectedBuildingToEdit.toObservable.map(_.flatMap(_.address).getOrElse("")),
                        onInput.mapToValue
                          .map(v =>
                            selectedBuildingToEdit.now().map(_.copy(address = Some(v)))
                          ) --> selectedBuildingToEdit
                      )
                    ),
                    td(
                      width("15%"),
                      div(
                        width("70%"),
                        float("left"),
                        button(
                          "Save",
                          className := "btn btn-primary",
                          onClickUpdateBuilding(selectedBuildingToEdit, buildingsService)
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
            )
        ),
        componentEventsHandler,
        pageEventsHandler,
        onMountCallback(_ => buildingsEventsObserver.onNext(BuildingsEvents.RefreshBuildingsWithFilters))
      )
    )

  def editBuildingButton(building: Building) =
    button(
      "Edit",
      className := "btn btn-warning",
      onClick --> { _ =>
        selectedBuildingToEdit.set(
          Some(
            SelectedBuildingEditForm(
              uuid = building.uuid,
              code = building.code,
              name = building.name,
              address = building.address
            )
          )
        )
      }
    )

  def editingEndedCleanup() =
    removeAllNotifications(editCodeNotifications, editBuildingNotifications)
    selectedBuildingToEdit.set(None)

  private def onClickUpdateBuilding(
    selectedBuildingEditForm: Var[Option[SelectedBuildingEditForm]],
    buildingsService: BuildingsService
  ) = thisEvents(onClick)
    .map { _ => selectedBuildingEditForm.now() }
    .collect { case Some(form) =>
      form
    }
    .flatMap { form =>
      if (form.code.isEmpty) {
        EventStream.fromValue(
          BuildingsTable.InvalidEditBuildingInputs(codeNotifications = List(Notification.Error("Missing code")))
        )
      } else {
        zio.Unsafe
          .unsafe {
            runtime.unsafe
              .run(
                buildingsService
                  .updateBuilding(
                    UpdateBuildingRequest(uuid = form.uuid, code = form.code, name = form.name, address = form.address),
                    token = userDetails.token
                  )
              )
              .getOrThrowFiberFailure()
          }
      }
    } --> componentEventBus

object BuildingsTable:
  case class InvalidEditBuildingInputs(codeNotifications: List[Notification])
