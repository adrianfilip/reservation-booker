package com.adrianfilip.booker.bk.infrastructure.domain

import com.adrianfilip.booker.bk.domain.DomainError.RoomDomainError
import com.adrianfilip.booker.bk.domain.RoomService
import com.adrianfilip.booker.bk.domain.repository.{BuildingRepository, RoomRepository}
import com.adrianfilip.booker.domain.model
import com.adrianfilip.booker.domain.model.{Building, Room}
import zio.{IO, UIO, ZIO, ZLayer}
private[infrastructure] case class RoomServiceLive(
  roomRepository: RoomRepository,
  buildingRepository: BuildingRepository
) extends RoomService:

  override def getFiltered(m: RoomService.GetFilteredRooms): UIO[List[Room]] =
    def toRepo = RoomRepository
      .LikeFilter(code = m.code, name = m.name, floor = m.floor, seats = m.seats, buildingId = m.buildingId)

    for
      roomEs <- roomRepository.findLike(toRepo)

      rooms <- ZIO.foreach(roomEs)(room =>
                 buildingRepository
                   .get(room.buildingId)
                   .orDie
                   .map(room.toModel)
               )
    yield rooms

  override def addRoom(m: RoomService.AddRoom): IO[RoomDomainError.RoomCodeAlreadyUsed, Unit] =
    roomRepository.add(
      RoomRepository
        .AddRoom(code = m.code, seats = m.seats, name = m.name, floor = m.floor, buildingId = m.buildingId)
    )

  override def updateRoom(
    m: RoomService.UpdateRoom
  ): IO[RoomDomainError.RoomNotFound | RoomDomainError.RoomCodeAlreadyUsed, Unit] =
    roomRepository.update(
      RoomRepository
        .RoomE(uuid = m.id, code = m.code, seats = m.seats, name = m.name, floor = m.floor, buildingId = m.buildingId)
    )

  override def getRoom(id: String): IO[RoomDomainError.RoomNotFound, Room] =
    for
      roomE    <- roomRepository
                    .findById(id)
                    .someOrFail(RoomDomainError.RoomNotFound(id))
      building <- buildingRepository.get(roomE.buildingId).orDie
    yield roomE.toModel(building)

  extension (room: RoomRepository.RoomE)
    private def toModel(building: Building): model.Room =
      model.Room(
        uuid = room.uuid,
        code = room.code,
        name = room.name,
        floor = room.floor,
        seats = room.seats,
        building = building
      )

object RoomServiceLive:
  val layer: ZLayer[RoomRepository with BuildingRepository, Nothing, RoomService] =
    ZLayer {
      for {
        roomRepository     <- ZIO.service[RoomRepository]
        buildingRepository <- ZIO.service[BuildingRepository]
      } yield RoomServiceLive(roomRepository, buildingRepository)
    }
