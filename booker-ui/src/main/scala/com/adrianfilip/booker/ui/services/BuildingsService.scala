package com.adrianfilip.booker.ui.services

import com.adrianfilip.booker.domain.model.Building
import com.adrianfilip.booker.scaleaware.building.{AddBuildingRequest, AddBuildingResponse, FindBuildingResponse, GetAllBuildingsResponse, GetFilteredBuildingsRequest, GetFilteredBuildingsResponse, UpdateBuildingRequest, UpdateBuildingResponse}
import com.adrianfilip.booker.ui.services.HttpClient.*
import com.raquo.airstream.core.EventStream
import io.laminext.fetch.Fetch
import zio.{Ref, Task, UIO, ZIO}
import zio.json.*

import java.util.UUID

trait BuildingsService:
  def getAllBuildings(token: String): UIO[EventStream[GetAllBuildingsResponse | ServiceErrors | SecurityErrors]]
  def getFilteredBuildings(
    m: GetFilteredBuildingsRequest,
    token: String
  ): UIO[EventStream[GetFilteredBuildingsResponse | ServiceErrors | SecurityErrors]]
  def addBuilding(
    input: AddBuildingRequest,
    token: String
  ): UIO[EventStream[AddBuildingResponse | ServiceErrors | SecurityErrors]]
  def updateBuilding(
    input: UpdateBuildingRequest,
    token: String
  ): UIO[EventStream[UpdateBuildingResponse | ServiceErrors | SecurityErrors]]
  def deleteBuilding(uuid: String): Task[Unit]
  def findBuilding(uuid: String): Task[Option[Building]]

object BuildingsService:

  val live: BuildingsService =
    new BuildingsService:
      def getAllBuildings(token: String): UIO[EventStream[GetAllBuildingsResponse | ServiceErrors | SecurityErrors]] =
        makeRequest[GetAllBuildingsResponse](
          Fetch
            .get(
              url = "http://localhost:8090/buildings",
              headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
            )
        )

      def getFilteredBuildings(
        m: GetFilteredBuildingsRequest,
        token: String
      ): UIO[EventStream[GetFilteredBuildingsResponse | ServiceErrors | SecurityErrors]] =
        makeRequest[GetFilteredBuildingsResponse](
          Fetch
            .post(
              "http://localhost:8090/buildings/filter",
              body = m.toJson,
              headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
            )
        )

      def addBuilding(
        m: AddBuildingRequest,
        token: String
      ): UIO[EventStream[AddBuildingResponse | ServiceErrors | SecurityErrors]] =
        makeRequest[
          AddBuildingResponse,
          AddBuildingResponse.AddBuildingResponseSuccess,
          AddBuildingResponse.AddBuildingResponseFailure
        ](
          Fetch
            .post(
              "http://localhost:8090/building",
              body = m.toJson,
              headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
            )
        )

      def updateBuilding(
        m: UpdateBuildingRequest,
        token: String
      ): UIO[EventStream[UpdateBuildingResponse | ServiceErrors | SecurityErrors]] =
        makeRequest[
          UpdateBuildingResponse,
          UpdateBuildingResponse.UpdateBuildingResponseSuccess,
          UpdateBuildingResponse.UpdateBuildingResponseFailure
        ](
          Fetch
            .put(
              "http://localhost:8090/building",
              body = m.toJson,
              headers = Map[String, String]("Authorization" -> s"""Bearer $token""")
            )
        )

      def deleteBuilding(uuid: String): Task[Unit] = ZIO.unit

      def findBuilding(uuid: String): Task[Option[Building]] = ZIO.succeedUnsafe(_ => None)
