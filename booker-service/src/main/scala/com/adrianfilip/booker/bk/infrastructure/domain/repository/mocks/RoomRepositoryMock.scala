package com.adrianfilip.booker.bk.infrastructure.domain.repository.mocks

import com.adrianfilip.booker.bk.domain.DomainError.RoomDomainError.{RoomCodeAlreadyUsed, RoomNotFound}
import com.adrianfilip.booker.bk.domain.repository.RoomRepository
import com.adrianfilip.booker.bk.domain.repository.RoomRepository.*
import com.adrianfilip.booker.domain.model.{Floor, Room}
import zio.stm.*
import zio.*

import java.util.UUID

object RoomRepositoryMock:
  val mock: ZLayer[TRef[Map[String, RoomE]], Nothing, RoomRepository] =
    ZLayer.fromZIO(for {
      roomsDb <- ZIO.service[TRef[Map[String, RoomE]]]
    } yield new RoomRepository:

      override def findLike(likeFilter: LikeFilter): UIO[List[RoomE]] =
        roomsDb.get
          .map(_.values.toList.filter { r =>
            likeFilter.code.forall(code => r.code.toLowerCase.contains(code.toLowerCase)) &&
            likeFilter.floor.forall(floor => r.floor == floor) &&
            likeFilter.name.forall(name => r.name.map(_.toLowerCase).exists(_.contains(name.toLowerCase))) &&
            likeFilter.buildingId.forall(bId => r.buildingId.contains(bId)) &&
            likeFilter.seats.forall(seats => r.seats >= seats)
          })
          .commit

      override def add(m: AddRoom): IO[RoomCodeAlreadyUsed, Unit] =
        STM.atomically {
          for {
            codeAlreadyUsed <- roomsDb.get.map(_.values.map(_.code).toList.contains(m.code))
            _               <- STM.when(codeAlreadyUsed)(STM.fail(RoomCodeAlreadyUsed(m.code)))
            newId            = UUID.randomUUID().toString
            _               <- roomsDb.update(
                                 _ + (newId -> RoomE(
                                   uuid = newId,
                                   code = m.code,
                                   name = m.name,
                                   floor = m.floor,
                                   seats = m.seats,
                                   buildingId = m.buildingId
                                 ))
                               )
          } yield ()
        }

      override def update(m: RoomE): IO[RoomNotFound | RoomCodeAlreadyUsed, Unit] =
        STM.atomically {
          for {
            codeAlreadyUsed <- roomsDb.get.map(_.values.filterNot(_.uuid == m.uuid).map(_.code).toList.contains(m.code))
            _               <- STM.when(codeAlreadyUsed)(STM.fail(RoomCodeAlreadyUsed(m.code)))
            room            <- roomsDb.get.map(_.get(m.uuid))
            _               <- STM.when(room.isEmpty)(STM.fail(RoomNotFound(m.uuid)))
            _               <- roomsDb.update(
                                 _ + (m.uuid -> RoomE(
                                   uuid = m.uuid,
                                   code = m.code,
                                   name = m.name,
                                   floor = m.floor,
                                   seats = m.seats,
                                   buildingId = m.buildingId
                                 ))
                               )
          } yield ()
        }

      override def findById(id: String): UIO[Option[RoomE]] =
        roomsDb.get.map(_.get(id)).commit
    )
