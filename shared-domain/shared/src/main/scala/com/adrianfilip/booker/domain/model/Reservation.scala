package com.adrianfilip.booker.domain.model

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.{LocalDate, LocalTime}

case class Reservation(
  id: String,
  seats: Int,
  date: LocalDate,
  duration: Int,
  startAt: LocalTime,
  endAt: LocalTime,
  building: Building,
  floor: Floor,
  room: Room,
  //userId
  holderId: String,
  cancelled: Boolean
)

object Reservation:
  implicit val codec: JsonCodec[Reservation] = DeriveJsonCodec.gen[Reservation]
