package com.adrianfilip.booker.bk.infrastructure.domain

import com.adrianfilip.booker.bk.domain.BuildingService
import com.adrianfilip.booker.bk.domain.DomainError.BuildingDomainError
import com.adrianfilip.booker.bk.domain.DomainError.BuildingDomainError.BuildingCodeAlreadyUsed
import com.adrianfilip.booker.bk.domain.repository.BuildingRepository
import com.adrianfilip.booker.bk.domain.repository.BuildingRepository.LikeFilter
import com.adrianfilip.booker.domain.model.Building
import zio.{IO, UIO, ZIO, ZLayer}

private[infrastructure] case class BuildingServiceLive(buildingRepository: BuildingRepository) extends BuildingService:
  override def getAll: UIO[List[Building]] =
    buildingRepository.findAll

  override def getFiltered(m: BuildingService.GetFilteredBuildings): UIO[List[Building]] =
    buildingRepository.findLike(LikeFilter(code = m.code, name = m.name, address = m.address))

  override def addBuilding(m: BuildingService.AddBuilding): IO[BuildingDomainError.BuildingCodeAlreadyUsed, Unit] =
    buildingRepository.add(BuildingRepository.AddBuilding(code = m.code, name = m.name, address = m.address))

  override def updateBuilding(
    m: BuildingService.UpdateBuilding
  ): IO[BuildingDomainError.BuildingNotFound | BuildingDomainError.BuildingCodeAlreadyUsed, Unit] =
    buildingRepository.update(Building(uuid = m.id, code = m.code, name = m.name, address = m.address))

  override def deleteBuilding(id: String): IO[BuildingDomainError.BuildingNotFound, Unit] =
    buildingRepository.delete(id)

  override def getBuilding(id: String): IO[BuildingDomainError.BuildingNotFound, Building] =
    buildingRepository
      .findById(id)
      .someOrFail(BuildingDomainError.BuildingNotFound(id))

object BuildingServiceLive:
  val layer: ZLayer[BuildingRepository, Nothing, BuildingService] =
    ZLayer {
      for {
        buildingRepository <- ZIO.service[BuildingRepository]
      } yield BuildingServiceLive(buildingRepository)
    }
