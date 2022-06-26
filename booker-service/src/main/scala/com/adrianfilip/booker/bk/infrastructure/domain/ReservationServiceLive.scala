package com.adrianfilip.booker.bk.infrastructure.domain

import com.adrianfilip.booker.bk.domain.DomainError.ReservationDomainError
import com.adrianfilip.booker.bk.domain.ReservationService
import com.adrianfilip.booker.bk.domain.repository.ReservationRepository.ReservationE
import com.adrianfilip.booker.bk.domain.repository.{BuildingRepository, ReservationRepository, RoomRepository}
import com.adrianfilip.booker.domain.model
import com.adrianfilip.booker.domain.model.{Reservation, Room}
import zio.{IO, UIO, ZIO, ZLayer}

object ReservationServiceLive:

  val live: ZLayer[ReservationRepository with BuildingRepository with RoomRepository, Nothing, ReservationService] =
    ZLayer.fromZIO(for {
      reservationRepository <- ZIO.service[ReservationRepository]
      buildingRepository    <- ZIO.service[BuildingRepository]
      roomRepository        <- ZIO.service[RoomRepository]
    } yield new ReservationService {
      override def getReservations(holderId: String): UIO[List[Reservation]] =
        for {
          reservationEs <- reservationRepository.getReservations(holderId)
          reservations  <-
            ZIO.foreach(reservationEs)(reservation =>
              for {
                building <- buildingRepository.get(reservation.buildingId).orDie
                room     <- roomRepository
                              .findById(reservation.roomId)
                              .flatMap(op => IO.fromOption(op).orElseFail(RuntimeException("not found")))
                              .orDie
              } yield model.Reservation(
                id = reservation.id,
                seats = reservation.seats,
                date = reservation.date,
                duration = reservation.duration,
                startAt = reservation.startAt,
                endAt = reservation.endAt,
                building = building,
                floor = reservation.floor,
                room = model.Room(
                  uuid = room.uuid,
                  code = room.code,
                  seats = room.seats,
                  name = room.name,
                  floor = room.floor,
                  building = building
                ),
                holderId = reservation.holderId,
                cancelled = reservation.cancelled
              )
            )
          sorted         = reservations.sortWith { (a, b) =>
                             if (b.date.compareTo(a.date) > 0) true
                             else if (b.startAt.compareTo(a.startAt) > 0) true
                             else false
                           }
        } yield sorted

      override def addReservation(m: ReservationService.AddReservation): IO[
        ReservationDomainError.ReservationConflict | ReservationDomainError.InsufficientCapacity |
          ReservationDomainError.RoomNotFound,
        Unit
      ] =
        for {
          reservations <- reservationRepository.getAllReservations
          room         <- roomRepository
                            .findById(m.roomId)
                            .flatMap(IO.fromOption(_))
                            .orElseFail(ReservationDomainError.RoomNotFound(m.roomId))
          _            <-
            ZIO.when(room.seats < m.seats)(IO.fail(ReservationDomainError.InsufficientCapacity(m.seats, room.seats)))
          conflicts     = computeConflicts(m, reservations)
          _            <- if (conflicts.nonEmpty) IO.fail(ReservationDomainError.ReservationConflict(conflicts))
                          else
                            reservationRepository.addReservation(
                              ReservationRepository.AddReservation(
                                seats = m.seats,
                                date = m.date,
                                duration = m.duration,
                                startAt = m.startAt,
                                endAt = m.endAt,
                                buildingId = m.buildingId,
                                floor = m.floor,
                                roomId = m.roomId,
                                holderId = m.holderId
                              )
                            )
        } yield ()

      private def computeConflicts(
        addReservation: ReservationService.AddReservation,
        reservations: List[ReservationE]
      ): List[ReservationDomainError.ConflictDetails] =
        reservations
          .filter(_.roomId == addReservation.roomId)
          .filter(_.date == addReservation.date)
          .filter { res =>
            val start  = res.startAt
            val end    = res.endAt
            val start2 = addReservation.startAt
            val end2   = addReservation.endAt
            (start2.compareTo(start) >= 0 && start2.compareTo(end) < 0) || (end2.compareTo(start) > 0 && end2
              .compareTo(end) <= 0)
          }
          .map(re =>
            ReservationDomainError.ConflictDetails(
              holderId = re.holderId,
              date = re.date,
              seats = re.seats,
              startAt = re.startAt,
              endAt = re.endAt
            )
          )

      override def cancel(id: String): IO[ReservationDomainError.ReservationNotFound, Unit] =
        for {
          _ <- reservationRepository.findReservation(id).flatMap(IO.fromOption(_)).orElseFail(ReservationDomainError.ReservationNotFound(id))
          _ <- reservationRepository.cancel(id)
        } yield ()

    })
