package com.adrianfilip.booker.domain.model

import zio.json.{DeriveJsonCodec, JsonCodec}

case class User(username: String, role: String, surname: String, lastName: String)
object User:
  implicit val codec: JsonCodec[User] = DeriveJsonCodec.gen[User]
