package com.adrianfilip.booker.ui.services

import com.adrianfilip.booker.domain.model.{Building, Floor, Room}
import com.adrianfilip.booker.scaleaware.building.GetFilteredBuildingsResponse
import com.adrianfilip.booker.scaleaware.room.{
  AddRoomRequest,
  AddRoomResponse,
  GetFilteredRoomsRequest,
  GetFilteredRoomsResponse,
  GetRoomResponse,
  GetRoomsResponse,
  UpdateRoomRequest,
  UpdateRoomResponse
}
import com.adrianfilip.booker.ui.services.HttpClient.makeRequest
import com.raquo.airstream.core.EventStream
import io.laminext.fetch.Fetch
import zio.*
import zio.json.*

import java.util.UUID

trait RoomsService:
  def getFilteredRooms(
    m: GetFilteredRoomsRequest,
    token: String
  ): UIO[EventStream[GetFilteredRoomsResponse | ServiceErrors | SecurityErrors]]
  def addRoom(m: AddRoomRequest, token: String): UIO[EventStream[AddRoomResponse | ServiceErrors | SecurityErrors]]
  def updateRoom(
    input: UpdateRoomRequest,
    token: String
  ): UIO[EventStream[UpdateRoomResponse | ServiceErrors | SecurityErrors]]
  def getRoom(id: String, token: String): UIO[EventStream[GetRoomResponse | ServiceErrors | SecurityErrors]]

object RoomsService:

  val mockRoomsService = new RoomsService {

    override def getFilteredRooms(
      m: GetFilteredRoomsRequest,
      token: String
    ): UIO[EventStream[GetFilteredRoomsResponse | ServiceErrors | SecurityErrors]] =
      makeRequest[
        GetFilteredRoomsResponse,
        GetFilteredRoomsResponse.GetFilteredRoomsResponseSuccess,
        GetFilteredRoomsResponse.GetFilteredRoomsResponseFailure
      ](
        Fetch
          .post(
            "http://localhost:8090/rooms/filter",
            body = m.toJson,
            headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
          )
      )

    override def addRoom(
      m: AddRoomRequest,
      token: String
    ): UIO[EventStream[AddRoomResponse | ServiceErrors | SecurityErrors]] =
      makeRequest[AddRoomResponse, AddRoomResponse.AddRoomResponseSuccess, AddRoomResponse.AddRoomResponseFailure](
        Fetch
          .post(
            "http://localhost:8090/room",
            body = m.toJson,
            headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
          )
      )

    override def getRoom(
      id: String,
      token: String
    ): UIO[EventStream[GetRoomResponse | ServiceErrors | SecurityErrors]] =
      makeRequest[GetRoomResponse, GetRoomResponse.GetRoomResponseSuccess, GetRoomResponse.GetRoomResponseFailure](
        Fetch
          .get(
            url = s"http://localhost:8090/room/$id",
            headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
          )
      )

    override def updateRoom(
      m: UpdateRoomRequest,
      token: String
    ): UIO[EventStream[UpdateRoomResponse | ServiceErrors | SecurityErrors]] =
      makeRequest[
        UpdateRoomResponse,
        UpdateRoomResponse.UpdateRoomResponseSuccess,
        UpdateRoomResponse.UpdateRoomResponseFailure
      ](
        Fetch
          .put(
            "http://localhost:8090/room",
            body = m.toJson,
            headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
          )
      )

  }
