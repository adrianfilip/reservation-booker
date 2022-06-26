package com.adrianfilip.booker.bk.domain

import com.adrianfilip.booker.bk.domain.BuildingService.{AddBuilding, GetFilteredBuildings, UpdateBuilding}
import com.adrianfilip.booker.bk.domain.DomainError.BuildingDomainError.*
import com.adrianfilip.booker.domain.model.Building
import zio.{IO, UIO, ZIO}

trait BuildingService:
  def getAll: UIO[List[Building]]
  def getFiltered(m: GetFilteredBuildings): UIO[List[Building]]
  def addBuilding(m: AddBuilding): IO[BuildingCodeAlreadyUsed, Unit]
  def updateBuilding(m: UpdateBuilding): IO[BuildingNotFound | BuildingCodeAlreadyUsed, Unit]
  def deleteBuilding(id: String): IO[BuildingNotFound, Unit]
  def getBuilding(id: String): IO[BuildingNotFound, Building]

object BuildingService:
  case class GetFilteredBuildings(code: Option[String], name: Option[String], address: Option[String])
  case class AddBuilding(code: String, name: Option[String], address: Option[String])
  case class UpdateBuilding(id: String, code: String, name: Option[String], address: Option[String])

  def getAll: ZIO[BuildingService, Nothing, List[Building]] =
    ZIO.serviceWithZIO[BuildingService](_.getAll)

  def getFiltered(m: GetFilteredBuildings): ZIO[BuildingService, Nothing, List[Building]] =
    ZIO.serviceWithZIO[BuildingService](_.getFiltered(m))

  def addBuilding(m: AddBuilding): ZIO[BuildingService, BuildingCodeAlreadyUsed, Unit] =
    ZIO.serviceWithZIO[BuildingService](_.addBuilding(m))

  def updateBuilding(m: UpdateBuilding): ZIO[BuildingService, BuildingNotFound | BuildingCodeAlreadyUsed, Unit] =
    ZIO.serviceWithZIO[BuildingService](_.updateBuilding(m))

  def deleteBuilding(id: String): ZIO[BuildingService, BuildingNotFound, Unit] =
    ZIO.serviceWithZIO[BuildingService](_.deleteBuilding(id))

  def getBuilding(id: String): ZIO[BuildingService, BuildingNotFound, Building] =
    ZIO.serviceWithZIO[BuildingService](_.getBuilding(id))
