package com.adrianfilip.booker.bk.infrastructure.domain.repository.mocks

import com.adrianfilip.booker.bk.domain.repository.ReservationRepository
import com.adrianfilip.booker.bk.domain.repository.ReservationRepository.ReservationE
import zio.*
import zio.stm.*

import java.util.UUID

object ReservationRepositoryMock:

  val mock: ZLayer[TRef[Map[String, ReservationE]], Nothing, ReservationRepository] =
    ZLayer.fromZIO(for {
      reservationsDB <- ZIO.service[TRef[Map[String, ReservationE]]]
    } yield new ReservationRepository:
      override def getReservations(holderId: String): UIO[List[ReservationE]] =
        reservationsDB.get.map(_.values.toList.filter(_.holderId == holderId)).commit

      override def findReservation(id: String): UIO[Option[ReservationE]] = 
        reservationsDB.get.map(_.get(id)).commit
           
      override def getAllReservations: UIO[List[ReservationE]] =
        reservationsDB.get.map(_.values.toList).commit

      override def cancel(id: String): UIO[Unit] =
        reservationsDB.update(_.filterNot(_._1 == id)).commit

      override def addReservation(addReservation: ReservationRepository.AddReservation): UIO[Unit] =
        STM.atomically {
          for {
            newId <- STM.succeed(UUID.randomUUID().toString)
            _     <- reservationsDB.update(
                       _ + (newId -> ReservationE(
                         id = newId,
                         seats = addReservation.seats,
                         date = addReservation.date,
                         duration = addReservation.duration,
                         startAt = addReservation.startAt,
                         endAt = addReservation.endAt,
                         buildingId = addReservation.buildingId,
                         floor = addReservation.floor,
                         roomId = addReservation.roomId,
                         holderId = addReservation.holderId,
                         cancelled = false
                       ))
                     )
          } yield ()
        }
    )
