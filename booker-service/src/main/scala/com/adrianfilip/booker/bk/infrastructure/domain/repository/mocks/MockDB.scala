package com.adrianfilip.booker.bk.infrastructure.domain.repository.mocks

import com.adrianfilip.booker.bk.domain.repository.ReservationRepository.ReservationE
import com.adrianfilip.booker.bk.domain.repository.RoomRepository.RoomE
import com.adrianfilip.booker.domain.model.{Building, Floor}
import zio.*
import zio.stm.TRef

import java.time.{LocalDate, LocalTime}
import java.util.UUID

object MockDB:
  val facultyOfComputerScienceId = UUID.randomUUID().toString
  val facultyOfMathematicsId     = UUID.randomUUID().toString

  val buildingsMockDB: ZLayer[Any, Nothing, TRef[Map[String, Building]]] =
    val facultyOfComputerScience  =
      Building(facultyOfComputerScienceId, "FC", Some("Faculty of Computer Science"), Some("Main Street no. 1"))
    val medicalSchool             =
      Building(UUID.randomUUID().toString, "MD", Some("Medical School"), Some("Main Street no. 2"))
    val lawSchool                 =
      Building(UUID.randomUUID().toString, "LS", Some("Law School"), Some("Second Street no. 3"))
    val facultyOfEconomics        =
      Building(UUID.randomUUID().toString, "FE", Some("Faculty of Economics"), Some("Main Street no. 4"))
    val facultyOfMathematics      =
      Building(facultyOfMathematicsId, "FM", Some("Faculty of Mathematics"), Some("Second Street no. 5"))
    val theoreticalPhysicsCollege =
      Building(UUID.randomUUID().toString, "TPC", Some("Theoretical Physics College"), Some("Main Street no. 6"))

    ZLayer.fromZIO(
      TRef
        .make(
          Map(
            facultyOfComputerScience.uuid  -> facultyOfComputerScience,
            medicalSchool.uuid             -> medicalSchool,
            lawSchool.uuid                 -> lawSchool,
            facultyOfEconomics.uuid        -> facultyOfEconomics,
            facultyOfMathematics.uuid      -> facultyOfMathematics,
            theoreticalPhysicsCollege.uuid -> theoreticalPhysicsCollege
          )
        )
        .commit
    )

  val lab1Id                                                      = UUID.randomUUID().toString
  val lab2Id                                                      = UUID.randomUUID().toString
  val lab4Id                                                      = UUID.randomUUID().toString
  val roomM113                                                    = RoomE(
    uuid = lab1Id,
    code = "M113",
    name = Some("Amphitheater 1"),
    floor = Floor.GroundFloor,
    buildingId = facultyOfComputerScienceId,
    seats = 40
  )
  val lab2                                                        =
    RoomE(
      uuid = lab2Id,
      code = "LAB-2",
      name = None,
      floor = Floor.GroundFloor,
      buildingId = facultyOfComputerScienceId,
      seats = 30
    )
  val roomM114                                                    =
    RoomE(
      uuid = UUID.randomUUID().toString,
      code = "M114",
      name = Some("Class room 1"),
      floor = Floor.Floor1,
      buildingId = facultyOfComputerScienceId,
      seats = 120
    )
  val lab3                                                        =
    RoomE(
      uuid = UUID.randomUUID().toString,
      code = "LAB-3",
      name = None,
      floor = Floor.Floor1,
      buildingId = facultyOfComputerScienceId,
      seats = 30
    )
  val lab4                                                        =
    RoomE(
      uuid = lab4Id,
      code = "LAB-4",
      name = None,
      floor = Floor.Floor2,
      buildingId = facultyOfMathematicsId,
      seats = 30
    )
  val roomsMockDB: ZLayer[Any, Nothing, TRef[Map[String, RoomE]]] =
    ZLayer.fromZIO(
      TRef
        .make(
          Map(
            roomM113.uuid -> roomM113,
            lab2.uuid     -> lab2,
            roomM114.uuid -> roomM114,
            lab3.uuid     -> lab3,
            lab4.uuid     -> lab4
          )
        )
        .commit
    )

  val reservation1Id                                                            = UUID.randomUUID().toString
  val reservationsMockDB: ZLayer[Any, Nothing, TRef[Map[String, ReservationE]]] =
    ZLayer.fromZIO(
      TRef
        .make(
          Map(
            reservation1Id -> ReservationE(
              id = reservation1Id,
              seats = roomM113.seats - 25,
              date = LocalDate.now(),
              duration = 4,
              startAt = LocalTime.of(8, 0),
              endAt = LocalTime.of(12, 0),
              buildingId = roomM113.buildingId,
              floor = roomM113.floor,
              roomId = roomM113.uuid,
              holderId = "john",
              cancelled = false
            ), {
              val rid = UUID.randomUUID().toString
              rid -> ReservationE(
                id = rid,
                seats = lab2.seats - 5,
                date = LocalDate.now().plusDays(1),
                duration = 4,
                startAt = LocalTime.of(8, 0),
                endAt = LocalTime.of(12, 0),
                buildingId = lab2.buildingId,
                floor = lab2.floor,
                roomId = lab2.uuid,
                holderId = "john",
                cancelled = false
              )
            }, {
              val rid = UUID.randomUUID().toString
              rid -> ReservationE(
                id = rid,
                seats = lab4.seats - 10,
                date = LocalDate.now().plusDays(2),
                duration = 4,
                startAt = LocalTime.of(8, 0),
                endAt = LocalTime.of(12, 0),
                buildingId = lab4.buildingId,
                floor = lab4.floor,
                roomId = lab4.uuid,
                holderId = "john",
                cancelled = false
              )
            }, {
              val rid = UUID.randomUUID().toString
              rid -> ReservationE(
                id = rid,
                seats = roomM114.seats - 15,
                date = LocalDate.now().plusDays(3),
                duration = 4,
                startAt = LocalTime.of(8, 0),
                endAt = LocalTime.of(12, 0),
                buildingId = roomM114.buildingId,
                floor = roomM114.floor,
                roomId = roomM114.uuid,
                holderId = "john",
                cancelled = false
              )
            }
          )
        )
        .commit
    )
