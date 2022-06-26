package com.adrianfilip.booker.bk

import com.adrianfilip.booker.bk.OuterError.MissingAuthorizationToken
import com.adrianfilip.booker.bk.domain.{
  AuthenticationService,
  BuildingService,
  DomainError,
  ReservationService,
  RoomService
}
import com.adrianfilip.booker.bk.domain.DomainError.AuthenticationDomainError.{
  InvalidPassword,
  TokenExpired,
  TokenInvalid,
  UserNotFound
}
import com.adrianfilip.booker.bk.domain.DomainError.BuildingDomainError.{BuildingCodeAlreadyUsed, BuildingNotFound}
import com.adrianfilip.booker.bk.domain.DomainError.RoomDomainError.{RoomCodeAlreadyUsed, RoomNotFound}
import com.adrianfilip.booker.bk.domain.DomainError.{
  AuthenticationDomainError,
  JWTTokenDomainError,
  ReservationDomainError,
  RoomDomainError
}
import com.adrianfilip.booker.bk.domain.repository.UserRepository
import com.adrianfilip.booker.bk.infrastructure.authentication.AuthenticationOperations
import com.adrianfilip.booker.bk.infrastructure.{AESEncryptService, JWTTokenService, JWTTokenServiceLive}
import com.adrianfilip.booker.bk.infrastructure.domain.repository.UserRepositoryMock
import com.adrianfilip.booker.bk.infrastructure.domain.repository.mocks.{
  BuildingRepositoryMock,
  MockDB,
  ReservationRepositoryMock,
  RoomRepositoryMock
}
import com.adrianfilip.booker.bk.infrastructure.domain.{
  AuthenticationServiceLive,
  BuildingServiceLive,
  ReservationServiceLive,
  RoomServiceLive
}
import com.adrianfilip.booker.domain.model.{Building, User}
import com.adrianfilip.booker.scaleaware.appresponse.FailedResponse
import com.adrianfilip.booker.scaleaware.authentication.{LoginRequest, LoginResult, LogoutRequest, LogoutResult}
import com.adrianfilip.booker.scaleaware.building.{
  AddBuildingRequest,
  AddBuildingResponse,
  GetAllBuildingsResponse,
  GetFilteredBuildingsRequest,
  GetFilteredBuildingsResponse,
  UpdateBuildingRequest,
  UpdateBuildingResponse
}
import com.adrianfilip.booker.scaleaware.reservation.{AddReservationRequest, CancelReservationResponse}
import com.adrianfilip.booker.scaleaware.reservation.AddReservationResponse.{
  AddReservationResponseFailure,
  AddReservationResponseSuccess
}
import com.adrianfilip.booker.scaleaware.room.{
  AddRoomRequest,
  AddRoomResponse,
  GetFilteredRoomsRequest,
  GetFilteredRoomsResponse,
  GetRoomResponse,
  UpdateRoomRequest,
  UpdateRoomResponse
}
import com.adrianfilip.booker.scaleaware.reservation.GetReservationsResponse.GetReservationsResponseSuccess
import zhttp.http.Middleware.bearerAuthZIO
import zhttp.http.*
import zhttp.http.Middleware.cors
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.Server
import zio.{Console, UIO, ZEnv, ZIO, ZIOAppDefault, ZLayer}
import zio.json.*
import zio.stm.TRef

import java.time.Clock

object BookerBackendApp extends ZIOAppDefault {

  val config: CorsConfig =
    CorsConfig(allowedOrigins = _ == "localhost", allowedMethods = Some(Set(Method.PUT, Method.DELETE)))

  // Http app that is accessible only via a jwt token
  def userApp: Http[AuthenticationService, Throwable, Request, Response] =
    Http.collectZIO[Request] { case req @ Method.POST -> !! / "logout" =>
      for {
        _   <- AuthenticationOperations.authenticate(req)
        //todo - call authenticationService.logout here if I want smth to happen on the backend side
        res <- ZIO.attempt(
                 Response.json(
                   LogoutResult
                     .SuccessLogoutResult()
                     .toJson
                 )
               )
      } yield res
    } @@ bearerAuthZIO[AuthenticationService, AuthenticationDomainError](s => AuthenticationOperations.bearerAuthZIO(s))

  // Http app that is accessible only via a jwt token
  def buildingApp: Http[AuthenticationService with BuildingService, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.GET -> !! / "buildings"                =>
        for {
          res <- BuildingService.getAll
        } yield Response.json(GetAllBuildingsResponse(buildings = res).toJson)
      case req @ Method.POST -> !! / "buildings" / "filter"    =>
        for {
          request <- AuthenticationOperations.parseRequest[GetFilteredBuildingsRequest](req)
          res     <-
            BuildingService.getFiltered(
              BuildingService.GetFilteredBuildings(code = request.code, name = request.name, address = request.address)
            )
        } yield Response.json(GetFilteredBuildingsResponse(buildings = res).toJson)
      case req @ Method.POST -> !! / "building"                =>
        for {
          request  <- AuthenticationOperations.parseRequest[AddBuildingRequest](req)
          response <-
            BuildingService
              .addBuilding(
                BuildingService.AddBuilding(code = request.code, name = request.name, address = request.address)
              )
              .as(Response.json(AddBuildingResponse.AddBuildingResponseSuccess().toJson))
              .catchSome { case BuildingCodeAlreadyUsed(code) =>
                ZIO.succeed(
                  Response.apply(
                    status = Status.ExpectationFailed,
                    data = HttpData.fromString(
                      AddBuildingResponse.AddBuildingResponseFailure("building.code.already.used", Some(code)).toJson
                    )
                  )
                )
              }
        } yield response
      case req @ Method.PUT -> !! / "building"                 =>
        for {
          request  <- AuthenticationOperations.parseRequest[UpdateBuildingRequest](req)
          response <-
            BuildingService
              .updateBuilding(
                BuildingService
                  .UpdateBuilding(
                    id = request.uuid,
                    code = request.code,
                    name = request.name,
                    address = request.address
                  )
              )
              .as(Response.json(UpdateBuildingResponse.UpdateBuildingResponseSuccess().toJson))
              .catchSome {
                case BuildingCodeAlreadyUsed(code) =>
                  ZIO.succeed(
                    Response.apply(
                      status = Status.ExpectationFailed,
                      data = HttpData.fromString(
                        UpdateBuildingResponse
                          .UpdateBuildingResponseFailure("building.code.already.used", Some(code))
                          .toJson
                      )
                    )
                  )
                case BuildingNotFound(id)          =>
                  ZIO.succeed(
                    Response.apply(
                      status = Status.NotFound,
                      data = HttpData.fromString(
                        UpdateBuildingResponse.UpdateBuildingResponseFailure("building.not.found", Some(id)).toJson
                      )
                    )
                  )
              }
        } yield response
    } @@ bearerAuthZIO[AuthenticationService, AuthenticationDomainError](s => AuthenticationOperations.bearerAuthZIO(s))

  def reservationApp: Http[AuthenticationService with ReservationService, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case _ @Method.GET -> !! / "reservations" / holderId =>
        ReservationService
          .getReservations(holderId)
          .map(rs => Response.json(GetReservationsResponseSuccess(rs).toJson))
      case _ @Method.DELETE -> !! / "reservation" / id     =>
        for {
          res <-
            ReservationService
              .cancel(id)
              .map(_ => Response.json(CancelReservationResponse.CancelReservationResponseSuccess().toJson))
              .catchSome { case ReservationDomainError.ReservationNotFound(id) =>
                ZIO.succeed(
                  Response.apply(
                    status = Status.NotFound,
                    data = HttpData.fromString(
                      CancelReservationResponse
                        .CancelReservationResponseFailure("reservation.not.found", Some(s"Reservation not found $id"))
                        .toJson
                    )
                  )
                )
              }
        } yield res
      case req @ Method.POST -> !! / "reservation"         =>
        for {
          request <- AuthenticationOperations.parseRequest[AddReservationRequest](req)
          res     <- ReservationService
                       .addReservation(
                         ReservationService.AddReservation(
                           seats = request.seats,
                           date = request.date,
                           duration = request.duration,
                           startAt = request.startTime,
                           endAt = request.endTime,
                           buildingId = request.buildingId,
                           floor = request.floor,
                           roomId = request.roomId,
                           holderId = request.holderId
                         )
                       )
                       .as(Response.json(AddReservationResponseSuccess().toJson))
                       .catchSome {
                         case ReservationDomainError.ReservationConflict(conflicts)             =>
                           ZIO.succeed(
                             Response.apply(
                               status = Status.Conflict,
                               data = HttpData.fromString(
                                 AddReservationResponseFailure(
                                   "reservation.conflict",
                                   Some(conflicts.map(_.toString).mkString("|||"))
                                 ).toJson
                               )
                             )
                           )
                         case ReservationDomainError.InsufficientCapacity(requested, available) =>
                           ZIO.succeed(
                             Response.apply(
                               status = Status.ExpectationFailed,
                               data = HttpData.fromString(
                                 AddReservationResponseFailure(
                                   "reservation.insufficient.capacity",
                                   Some(s"Requested: $requested seats but room has Available: $available")
                                 ).toJson
                               )
                             )
                           )
                         case ReservationDomainError.RoomNotFound(id)                           =>
                           ZIO.succeed(
                             Response.apply(
                               status = Status.ExpectationFailed,
                               data = HttpData.fromString(
                                 AddReservationResponseFailure(
                                   "reservation.room.not.found",
                                   Some(s"Room with id: $id not found")
                                 ).toJson
                               )
                             )
                           )
                       }
        } yield res
    } @@ bearerAuthZIO[AuthenticationService, AuthenticationDomainError](s => AuthenticationOperations.bearerAuthZIO(s))

  def roomApp: Http[AuthenticationService with RoomService, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "rooms" / "filter" =>
        for {
          request <- AuthenticationOperations.parseRequest[GetFilteredRoomsRequest](req)
          res     <-
            RoomService
              .getFiltered(
                RoomService.GetFilteredRooms(
                  code = request.code,
                  name = request.name,
                  floor = request.floor,
                  seats = request.seats,
                  buildingId = request.buildingId
                )
              )
              .map(ls => Response.json(GetFilteredRoomsResponse.GetFilteredRoomsResponseSuccess(rooms = ls).toJson))
              .catchSome { case RoomDomainError.BuildingNotFound(id) =>
                ZIO.succeed(
                  Response.apply(
                    status = Status.ExpectationFailed,
                    data = HttpData.fromString(
                      GetFilteredRoomsResponse
                        .GetFilteredRoomsResponseFailure("building.not.found", Some(id))
                        .toJson
                    )
                  )
                )
              }
        } yield res
      case req @ Method.GET -> !! / "room" / roomId     =>
        for {
          response <-
            RoomService
              .getRoom(roomId)
              .map(room => Response.json(GetRoomResponse.GetRoomResponseSuccess(room).toJson))
              .catchSome { case RoomNotFound(id) =>
                ZIO.succeed(
                  Response.apply(
                    status = Status.NotFound,
                    data =
                      HttpData.fromString(GetRoomResponse.GetRoomResponseFailure("room.not.found", Some(id)).toJson)
                  )
                )
              }
        } yield response
      case req @ Method.POST -> !! / "room"             =>
        for {
          request  <- AuthenticationOperations.parseRequest[AddRoomRequest](req)
          response <-
            RoomService
              .addRoom(
                RoomService.AddRoom(
                  code = request.code,
                  seats = request.seats,
                  name = request.name,
                  floor = request.floor,
                  buildingId = request.buildingId
                )
              )
              .as(Response.json(AddRoomResponse.AddRoomResponseSuccess().toJson))
              .catchSome { case RoomCodeAlreadyUsed(code) =>
                ZIO.succeed(
                  Response.apply(
                    status = Status.ExpectationFailed,
                    data = HttpData
                      .fromString(AddRoomResponse.AddRoomResponseFailure("room.code.already.used", Some(code)).toJson)
                  )
                )
              }
        } yield response
      case req @ Method.PUT -> !! / "room"              =>
        for {
          request  <- AuthenticationOperations.parseRequest[UpdateRoomRequest](req)
          response <-
            RoomService
              .updateRoom(
                RoomService.UpdateRoom(
                  id = request.uuid,
                  code = request.code,
                  seats = request.seats,
                  name = request.name,
                  floor = request.floor,
                  buildingId = request.buildingId
                )
              )
              .as(Response.json(UpdateRoomResponse.UpdateRoomResponseSuccess().toJson))
              .catchSome {
                case RoomCodeAlreadyUsed(code) =>
                  ZIO.succeed(
                    Response.apply(
                      status = Status.ExpectationFailed,
                      data = HttpData.fromString(
                        UpdateRoomResponse.UpdateRoomResponseFailure("room.code.already.used", Some(code)).toJson
                      )
                    )
                  )
                case RoomNotFound(id)          =>
                  ZIO.succeed(
                    Response.apply(
                      status = Status.NotFound,
                      data = HttpData
                        .fromString(UpdateRoomResponse.UpdateRoomResponseFailure("room.not.found", Some(id)).toJson)
                    )
                  )
              }
        } yield response
    } @@ bearerAuthZIO[AuthenticationService, AuthenticationDomainError](s => AuthenticationOperations.bearerAuthZIO(s))

  def loginApp: Http[AuthenticationService, Throwable, Request, Response] =
    Http
      .collectZIO[Request] { case req @ Method.POST -> !! / "login" =>
        for {
          input  <- req.bodyAsString
                      .flatMap(s => ZIO.fromEither(s.fromJson[LoginRequest]))
                      .mapError(s => OuterError.BadRequestError(s.toString))
          result <-
            ZIO
              .serviceWithZIO[AuthenticationService](_.login(input.username, input.password))
              .map(res =>
                Response.json(
                  LoginResult
                    .SuccessfulLoginResult(
                      User(
                        username = res.user.username,
                        role = res.user.role,
                        surname = res.user.surname,
                        lastName = res.user.lastName
                      ),
                      res.token
                    )
                    .toJson
                )
              )
              .catchSome {
                case InvalidPassword        =>
                  makeFailedResponseZIO(Status.Forbidden, FailedResponse("invalid.password", None))
                case UserNotFound(username) =>
                  makeFailedResponseZIO(
                    Status.NotFound,
                    FailedResponse("user.not.found", Some(s"No user with username $username"))
                  )
              }
        } yield result
      }

  val appLayer
    : ZLayer[Any, Nothing, AuthenticationService with BuildingService with RoomService with ReservationService] =
    ZLayer.make[AuthenticationService with BuildingService with RoomService with ReservationService](
      ZLayer.succeed(java.time.Clock.systemUTC),
      JWTTokenServiceLive.live,
      UserRepositoryMock.mock,
      AESEncryptService.live,
      AuthenticationServiceLive.live,
      BuildingRepositoryMock.mock,
      BuildingServiceLive.live,
      RoomRepositoryMock.mock,
      RoomServiceLive.live,
      ReservationRepositoryMock.mock,
      ReservationServiceLive.live,
      MockDB.roomsMockDB,
      MockDB.buildingsMockDB,
      MockDB.reservationsMockDB
    )

  // Composing all the HttpApps together
  val app: Http[
    AuthenticationService with RoomService with BuildingService with ReservationService,
    Throwable,
    Request,
    Response
  ] = (
    loginApp ++ userApp ++ buildingApp ++ roomApp ++ reservationApp
  ).catchSome {
    case TokenExpired(m) =>
      makeFailedHttpResponse(Status.Forbidden, FailedResponse("token.expired", Some(m)))
    case TokenInvalid(m) =>
      makeFailedHttpResponse(Status.Forbidden, FailedResponse("token.invalid", Some(m)))
    case err: OuterError =>
      Http.succeed(err.toResponse)
    case de: DomainError =>
      makeFailedHttpResponse(Status.InternalServerError, FailedResponse("internal.error", Some(de.getClass.getName)))
  } @@ cors(config)

  def makeFailedHttpResponse(status: Status, failedResponse: FailedResponse): Http[Any, Nothing, Any, Response] =
    Http.succeed(makeFailedResponse(status, failedResponse))

  def makeFailedResponse(status: Status, failedResponse: FailedResponse): Response =
    Response.apply(status = status, data = HttpData.fromString(failedResponse.toJson))

  def makeFailedResponseZIO(status: Status, failedResponse: FailedResponse): UIO[Response] =
    ZIO.succeed(makeFailedResponse(status, failedResponse))

  override def run =
    Server
      .start[AuthenticationService with BuildingService with RoomService with ReservationService](8090, app)
      .provideLayer(appLayer)

}
