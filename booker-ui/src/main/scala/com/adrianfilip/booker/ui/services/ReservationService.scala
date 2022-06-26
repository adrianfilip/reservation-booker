package com.adrianfilip.booker.ui.services

import com.adrianfilip.booker.domain.model.{Building, Floor, Reservation, Room}
import com.adrianfilip.booker.scaleaware.reservation.{
  AddReservationRequest,
  AddReservationResponse,
  CancelReservationResponse,
  GetReservationsResponse
}
import com.adrianfilip.booker.ui.pages.myreservations.MakeReservation.Interval
import com.adrianfilip.booker.ui.services.HttpClient.makeRequest
import com.adrianfilip.booker.ui.services.ReservationService.AddReservation
import com.adrianfilip.components.datepicker.Datepicker.Day
import com.raquo.airstream.core.EventStream
import io.laminext.fetch.Fetch
import zio.{Random, Ref, UIO, ZIO}
import zio.json.*

import java.time.{LocalDate, LocalTime}

trait ReservationService:
  def addReservation(
    m: AddReservation,
    token: String
  ): UIO[EventStream[AddReservationResponse | ServiceErrors | SecurityErrors]]
  def getReservations(
    holderId: String,
    token: String
  ): UIO[EventStream[GetReservationsResponse | ServiceErrors | SecurityErrors]]
  def cancelReservation(
    id: String,
    token: String
  ): UIO[EventStream[CancelReservationResponse | ServiceErrors | SecurityErrors]]

object ReservationService:

  val live =
    new ReservationService:
      override def addReservation(
        m: AddReservation,
        token: String
      ): UIO[EventStream[AddReservationResponse | ServiceErrors | SecurityErrors]] =
        makeRequest[
          AddReservationResponse,
          AddReservationResponse.AddReservationResponseSuccess,
          AddReservationResponse.AddReservationResponseFailure
        ](
          Fetch
            .post(
              "http://localhost:8090/reservation",
              body = AddReservationRequest(
                seats = m.seats,
                date = LocalDate.of(m.date.year, m.date.month, m.date.day),
                duration = m.duration,
                startTime = m.interval.start,
                endTime = m.interval.end,
                buildingId = m.buildingId,
                floor = m.floor,
                roomId = m.roomId,
                holderId = m.holderId
              ).toJson,
              headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
            )
        )

      override def getReservations(
        holderId: String,
        token: String
      ): UIO[EventStream[GetReservationsResponse | ServiceErrors | SecurityErrors]] =
        makeRequest[
          GetReservationsResponse,
          GetReservationsResponse.GetReservationsResponseSuccess,
          GetReservationsResponse.GetReservationsResponseFailure
        ](
          Fetch
            .get(
              url = s"http://localhost:8090/reservations/$holderId",
              headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
            )
        )

      override def cancelReservation(
        id: String,
        token: String
      ): UIO[EventStream[CancelReservationResponse | ServiceErrors | SecurityErrors]] =
        makeRequest[
          CancelReservationResponse,
          CancelReservationResponse.CancelReservationResponseSuccess,
          CancelReservationResponse.CancelReservationResponseFailure
        ](
          Fetch
            .delete(
              url = s"http://localhost:8090/reservation/$id",
              headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
            )
        )

  case class AddReservation(
    seats: Int,
    date: Day,
    duration: Int,
    interval: Interval,
    buildingId: String,
    floor: Floor,
    roomId: String,
    holderId: String
  )
