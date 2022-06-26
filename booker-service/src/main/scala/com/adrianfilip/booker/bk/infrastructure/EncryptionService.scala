package com.adrianfilip.booker.bk.infrastructure

import zio.UIO

trait EncryptionService:
  def encrypt(str: String, key: String): UIO[String]
