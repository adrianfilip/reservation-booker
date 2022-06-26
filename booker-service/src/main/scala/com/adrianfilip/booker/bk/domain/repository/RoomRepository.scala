package com.adrianfilip.booker.bk.domain.repository

import com.adrianfilip.booker.bk.domain.DomainError.RoomDomainError.{RoomCodeAlreadyUsed, RoomNotFound}
import com.adrianfilip.booker.bk.domain.repository.RoomRepository.{AddRoom, LikeFilter, RoomE}
import com.adrianfilip.booker.domain.model.Floor
import zio.{IO, UIO, ZIO}

trait RoomRepository:
  def findLike(likeFilter: LikeFilter): UIO[List[RoomE]]
  def add(m: AddRoom): IO[RoomCodeAlreadyUsed, Unit]
  def update(m: RoomE): IO[RoomNotFound | RoomCodeAlreadyUsed, Unit]
  def findById(id: String): UIO[Option[RoomE]]

object RoomRepository:
  case class LikeFilter(
    code: Option[String],
    name: Option[String],
    floor: Option[Floor],
    seats: Option[Int],
    buildingId: Option[String]
  )
  case class AddRoom(code: String, seats: Int, name: Option[String], floor: Floor, buildingId: String)
  case class RoomE(uuid: String, code: String, seats: Int, name: Option[String], floor: Floor, buildingId: String)
