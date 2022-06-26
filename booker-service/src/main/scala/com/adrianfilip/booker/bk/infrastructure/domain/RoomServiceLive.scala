package com.adrianfilip.booker.bk.infrastructure.domain

import com.adrianfilip.booker.bk.domain.DomainError.RoomDomainError
import com.adrianfilip.booker.bk.domain.RoomService
import com.adrianfilip.booker.bk.domain.repository.{BuildingRepository, RoomRepository}
import com.adrianfilip.booker.domain.model
import com.adrianfilip.booker.domain.model.Room
import zio.{IO, UIO, ZIO, ZLayer}

object RoomServiceLive:
  val live: ZLayer[RoomRepository with BuildingRepository, Nothing, RoomService] =
    ZLayer.fromZIO(for {
      roomRepository     <- ZIO.service[RoomRepository]
      buildingRepository <- ZIO.service[BuildingRepository]
      roomService         =
        new RoomService:

          override def getFiltered(m: RoomService.GetFilteredRooms): UIO[List[Room]] =
            roomRepository
              .findLike(
                RoomRepository
                  .LikeFilter(code = m.code, name = m.name, floor = m.floor, seats = m.seats, buildingId = m.buildingId)
              )
              .flatMap(roomEs =>
                ZIO.foreach(roomEs)(room =>
                  buildingRepository
                    .get(room.buildingId)
                    .orDie
                    .map(building =>
                      model.Room(
                        uuid = room.uuid,
                        code = room.code,
                        name = room.name,
                        floor = room.floor,
                        seats = room.seats,
                        building = building
                      )
                    )
                )
              )

          override def addRoom(m: RoomService.AddRoom): IO[RoomDomainError.RoomCodeAlreadyUsed, Unit] =
            roomRepository.add(
              RoomRepository
                .AddRoom(code = m.code, seats = m.seats, name = m.name, floor = m.floor, buildingId = m.buildingId)
            )

          override def updateRoom(
            m: RoomService.UpdateRoom
          ): IO[RoomDomainError.RoomNotFound | RoomDomainError.RoomCodeAlreadyUsed, Unit] =
            roomRepository.update(
              RoomRepository.RoomE(
                uuid = m.id,
                code = m.code,
                seats = m.seats,
                name = m.name,
                floor = m.floor,
                buildingId = m.buildingId
              )
            )

          override def getRoom(id: String): IO[RoomDomainError.RoomNotFound, Room] =
            roomRepository
              .findById(id)
              .flatMap(IO.fromOption(_).orElseFail(RoomDomainError.RoomNotFound(id)))
              .flatMap { roomE =>
                buildingRepository
                  .get(roomE.buildingId)
                  .orDie
                  .map(building =>
                    model.Room(
                      uuid = roomE.uuid,
                      code = roomE.code,
                      name = roomE.name,
                      floor = roomE.floor,
                      seats = roomE.seats,
                      building = building
                    )
                  )
              }
    } yield roomService)
