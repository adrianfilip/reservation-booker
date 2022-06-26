package com.adrianfilip.booker.bk.domain.repository

import com.adrianfilip.booker.bk.domain.repository.ReservationRepository.{AddReservation, ReservationE}
import com.adrianfilip.booker.domain.model.Floor
import zio.UIO

import java.time.{LocalDate, LocalTime}

trait ReservationRepository:
  def getReservations(holderId: String): UIO[List[ReservationE]]
  def findReservation(id: String): UIO[Option[ReservationE]]
  def getAllReservations: UIO[List[ReservationE]]
  def addReservation(addReservation: AddReservation): UIO[Unit]
  def cancel(id: String): UIO[Unit]

object ReservationRepository:
  case class ReservationE(
    id: String,
    seats: Int,
    date: LocalDate,
    duration: Int,
    startAt: LocalTime,
    endAt: LocalTime,
    buildingId: String,
    floor: Floor,
    roomId: String,
    //userId
    holderId: String,
    cancelled: Boolean
  )
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
