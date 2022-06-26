package com.adrianfilip.booker.domain.model

import zio.json.*

case class Building(uuid: String, code: String, name: Option[String], address: Option[String])
object Building:
  implicit val codec: JsonCodec[Building] = DeriveJsonCodec.gen[Building]
