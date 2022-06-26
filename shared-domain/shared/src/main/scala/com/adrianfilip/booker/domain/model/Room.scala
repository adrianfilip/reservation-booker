package com.adrianfilip.booker.domain.model

import zio.json.{DeriveJsonCodec, JsonCodec}

case class Room(uuid: String, code: String, seats: Int, name: Option[String], floor: Floor, building: Building)
object Room:
  implicit val codec: JsonCodec[Room] = DeriveJsonCodec.gen[Room]

enum Floor:
  case Underground2, Underground1, GroundFloor, Floor1, Floor2, Floor3, Floor4, Floor5

object Floor:
  implicit val codec: JsonCodec[Floor] = DeriveJsonCodec.gen[Floor]
