package com.adrianfilip.booker.bk.domain

import java.time.{LocalDate, LocalTime}

sealed trait DomainError extends Throwable

object DomainError:
  sealed trait AuthenticationDomainError                                         extends DomainError
  sealed trait JWTTokenDomainError                                               extends DomainError
  sealed trait BuildingDomainError                                               extends DomainError
  sealed trait RoomDomainError                                                   extends DomainError
  sealed trait ReservationDomainError                                            extends DomainError
  final case class GenericDomainError(code: String, description: Option[String]) extends DomainError

  object AuthenticationDomainError:
    final case class UserNotFound(username: String) extends AuthenticationDomainError
    case object InvalidPassword                     extends AuthenticationDomainError
    final case class TokenInvalid(message: String)  extends AuthenticationDomainError
    final case class TokenExpired(message: String)  extends AuthenticationDomainError

  object JWTTokenDomainError:
    case object MalformedToken                  extends JWTTokenDomainError
    case class ExpiredToken(message: String)    extends JWTTokenDomainError
    case class InvalidJWTToken(message: String) extends JWTTokenDomainError

  object BuildingDomainError:
    final case class BuildingNotFound(id: String)          extends BuildingDomainError
    final case class BuildingCodeAlreadyUsed(code: String) extends BuildingDomainError

  object RoomDomainError:
    final case class RoomNotFound(id: String)          extends RoomDomainError
    final case class RoomCodeAlreadyUsed(code: String) extends RoomDomainError
    final case class BuildingNotFound(id: String)      extends RoomDomainError

  object ReservationDomainError:
    final case class ReservationNotFound(id: String)                                      extends ReservationDomainError
    final case class ReservationConflict(conflicts: List[ConflictDetails])                extends ReservationDomainError
    final case class InsufficientCapacity(requestedSeats: Int, availableSeats: Int) extends ReservationDomainError
    final case class ConflictDetails(
      holderId: String,
      date: LocalDate,
      seats: Int,
      startAt: LocalTime,
      endAt: LocalTime
    )
    final case class RoomNotFound(id: String)                                             extends ReservationDomainError
