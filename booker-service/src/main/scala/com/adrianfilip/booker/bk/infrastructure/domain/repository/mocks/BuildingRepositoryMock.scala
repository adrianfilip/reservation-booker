package com.adrianfilip.booker.bk.infrastructure.domain.repository.mocks

import com.adrianfilip.booker.bk.domain.DomainError.BuildingDomainError.{BuildingCodeAlreadyUsed, BuildingNotFound}
import com.adrianfilip.booker.bk.domain.repository.BuildingRepository
import com.adrianfilip.booker.bk.domain.repository.BuildingRepository.*
import com.adrianfilip.booker.domain.model.Building
import zio.stm.*
import zio.*

import java.util.UUID

object BuildingRepositoryMock:
  val mock: ZLayer[TRef[Map[String, Building]], Nothing, BuildingRepository] =
    ZLayer.fromZIO(for {
      buildingsDb <- ZIO.service[TRef[Map[String, Building]]]
    } yield new BuildingRepository:

      override def findAll: UIO[List[Building]] =
        buildingsDb.get
          .map(_.values.toList)
          .commit

      override def add(m: AddBuilding): IO[BuildingCodeAlreadyUsed, Unit] =
        STM.atomically {
          for {
            _ <- verifyCodeAlreadyExists(m.code)
            _ <- buildingsDb.update { db =>
                   val uuid = UUID.randomUUID().toString
                   db.+(uuid -> Building(uuid, m.code, m.name, m.address))
                 }
          } yield ()
        }

      override def findById(id: String): UIO[Option[Building]] =
        buildingsDb.get.map(_.values.toList.find(_.uuid == id)).commit

      override def update(m: Building): IO[BuildingNotFound | BuildingCodeAlreadyUsed, Unit] =
        STM.atomically {
          for {
            _ <- verifyCodeAlreadyExists(m.uuid, m.code)
            s <- buildingsDb.get.map(_.contains(m.uuid))
            _ <- STM.when(!s)(STM.fail[BuildingNotFound](BuildingNotFound(m.uuid)))
            _ <- buildingsDb
                   .getAndUpdate(db => db.+(m.uuid -> Building(m.uuid, m.code, m.name, m.address)))
          } yield ()
        }

      private def verifyCodeAlreadyExists(newCode: String): ZSTM[Any, BuildingCodeAlreadyUsed, Unit] =
        for {
          codeAlreadyExists <-
            buildingsDb.get
              .map(ls => ls.toList.map(_._2.code).contains(newCode))
          _                 <-
            STM.when(codeAlreadyExists) {
              STM.fail(BuildingCodeAlreadyUsed(newCode))
            }
        } yield ()

      private def verifyCodeAlreadyExists(updateId: String, newCode: String): ZSTM[Any, BuildingCodeAlreadyUsed, Unit] =
        for {
          codeAlreadyExists <-
            buildingsDb.get
              .map(ls => ls.toList.filterNot(_._1 == updateId).map(_._2.code).contains(newCode))
          _                 <-
            STM.when(codeAlreadyExists) {
              STM.fail(BuildingCodeAlreadyUsed(newCode))
            }
        } yield ()

      override def delete(id: String): IO[BuildingNotFound, Unit] =
        STM.atomically {
          for {
            s <- buildingsDb.get.map(_.contains(id))
            _ <- STM.when(!s)(STM.fail(BuildingNotFound(id)))
            _ <- buildingsDb.getAndUpdate(_ - id).unit
          } yield ()
        }

      override def get(id: String): IO[BuildingNotFound, Building] =
        buildingsDb.get
          .map(_.values.toList.find(_.uuid == id))
          .flatMap(STM.fromOption(_).orElseFail(BuildingNotFound(id)))
          .commit

      override def findLike(filters: LikeFilter): UIO[List[Building]] =
        buildingsDb.get
          .map(_.values.toList.filter { b =>
            filters.code.forall(code => b.code.toLowerCase.contains(code.toLowerCase)) &&
            filters.name.forall(name => b.name.map(_.toLowerCase).exists(_.contains(name.toLowerCase))) &&
            filters.address.forall(address => b.address.map(_.toLowerCase).exists(_.contains(address.toLowerCase)))
          })
          .commit
    )
