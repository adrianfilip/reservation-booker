package com.adrianfilip.booker.bk.domain.repository

import com.adrianfilip.booker.bk.domain.DomainError.BuildingDomainError.{BuildingCodeAlreadyUsed, BuildingNotFound}
import com.adrianfilip.booker.bk.domain.repository.BuildingRepository.{AddBuilding, LikeFilter}
import com.adrianfilip.booker.domain.model.Building
import zio.{IO, UIO, ZIO}

trait BuildingRepository:
  def findAll: UIO[List[Building]]
  def findLike(likeFilter: LikeFilter): UIO[List[Building]]
  def findById(id: String): UIO[Option[Building]]
  def get(id: String): IO[BuildingNotFound, Building]
  def add(m: AddBuilding): IO[BuildingCodeAlreadyUsed, Unit]
  def update(m: Building): IO[BuildingNotFound | BuildingCodeAlreadyUsed, Unit]
  def delete(id: String): IO[BuildingNotFound, Unit]

object BuildingRepository:
  case class LikeFilter(code: Option[String], name: Option[String], address: Option[String])
  case class AddBuilding(code: String, name: Option[String], address: Option[String])

  def findAll: ZIO[BuildingRepository, Nothing, List[Building]] =
    ZIO.serviceWithZIO[BuildingRepository](_.findAll)

  def findLike(likeFilter: LikeFilter): ZIO[BuildingRepository, Nothing, List[Building]] =
    ZIO.serviceWithZIO[BuildingRepository](_.findLike(likeFilter))

  def findById(id: String): ZIO[BuildingRepository, Nothing, Option[Building]] =
    ZIO.serviceWithZIO[BuildingRepository](_.findById(id))

  def add(m: AddBuilding): ZIO[BuildingRepository, BuildingCodeAlreadyUsed, Unit] =
    ZIO.serviceWithZIO[BuildingRepository](_.add(m))

  def update(m: Building): ZIO[BuildingRepository, BuildingNotFound | BuildingCodeAlreadyUsed, Unit] =
    ZIO.serviceWithZIO[BuildingRepository](_.update(m))

  def delete(id: String): ZIO[BuildingRepository, BuildingNotFound, Unit] =
    ZIO.serviceWithZIO[BuildingRepository](_.delete(id))

  def get(id: String): ZIO[BuildingRepository, BuildingNotFound, Building] =
    ZIO.serviceWithZIO[BuildingRepository](_.get(id))
