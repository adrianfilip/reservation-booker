package com.adrianfilip.booker.bk.domain

import com.adrianfilip.booker.bk.domain.DomainError.RoomDomainError.{
  BuildingNotFound,
  RoomCodeAlreadyUsed,
  RoomNotFound
}
import com.adrianfilip.booker.bk.domain.RoomService.{AddRoom, GetFilteredRooms, UpdateRoom}
import com.adrianfilip.booker.domain.model.{Floor, Room}
import zio.{IO, Task, UIO, ZIO}

trait RoomService:
  def getFiltered(m: GetFilteredRooms): IO[BuildingNotFound, List[Room]]
  def addRoom(m: AddRoom): IO[RoomCodeAlreadyUsed, Unit]
  def updateRoom(m: UpdateRoom): IO[RoomNotFound | RoomCodeAlreadyUsed, Unit]
  def getRoom(id: String): IO[RoomNotFound, Room]

object RoomService:
  case class GetFilteredRooms(
    code: Option[String],
    name: Option[String],
    floor: Option[Floor],
    seats: Option[Int],
    buildingId: Option[String]
  )

  case class AddRoom(code: String, seats: Int, name: Option[String], floor: Floor, buildingId: String)
  case class UpdateRoom(id: String, code: String, seats: Int, name: Option[String], floor: Floor, buildingId: String)

  def getFiltered(m: GetFilteredRooms): ZIO[RoomService, BuildingNotFound, List[Room]] =
    ZIO.serviceWithZIO[RoomService](_.getFiltered(m))

  def addRoom(m: AddRoom): ZIO[RoomService, RoomCodeAlreadyUsed, Unit] =
    ZIO.serviceWithZIO[RoomService](_.addRoom(m))

  def updateRoom(m: UpdateRoom): ZIO[RoomService, RoomNotFound | RoomCodeAlreadyUsed, Unit] =
    ZIO.serviceWithZIO[RoomService](_.updateRoom(m))

  def getRoom(id: String): ZIO[RoomService, RoomNotFound, Room] =
    ZIO.serviceWithZIO[RoomService](_.getRoom(id))
