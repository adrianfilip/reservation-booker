package com.adrianfilip.booker.bk.domain

import com.adrianfilip.booker.bk.domain.DomainError.ReservationDomainError
import com.adrianfilip.booker.bk.domain.ReservationService.AddReservation
import com.adrianfilip.booker.domain.model.{Floor, Reservation}
import zio.{IO, UIO, ZIO}

import java.time.{LocalDate, LocalTime}

trait ReservationService:
  def getReservations(holderId: String): UIO[List[Reservation]]
  def addReservation(m: AddReservation): IO[
    ReservationDomainError.ReservationConflict | ReservationDomainError.InsufficientCapacity |
      ReservationDomainError.RoomNotFound,
    Unit
  ]
  def cancel(id: String): IO[ReservationDomainError.ReservationNotFound, Unit]

object ReservationService:
  case class AddReservation(
    seats: Int,
    date: LocalDate,
    duration: Int,
    startAt: LocalTime,
    endAt: LocalTime,
    buildingId: String,
    floor: Floor,
    roomId: String,
    holderId: String
  )

  def getReservations(holderId: String): ZIO[ReservationService, Nothing, List[Reservation]] =
    ZIO.serviceWithZIO[ReservationService](_.getReservations(holderId))

  def addReservation(m: AddReservation): ZIO[
    ReservationService,
    ReservationDomainError.ReservationConflict | ReservationDomainError.InsufficientCapacity |
      ReservationDomainError.RoomNotFound,
    Unit
  ] =
    ZIO.serviceWithZIO[ReservationService](_.addReservation(m))

  def cancel(id: String): ZIO[ReservationService, ReservationDomainError.ReservationNotFound, Unit] =
    ZIO.serviceWithZIO[ReservationService](_.cancel(id))
