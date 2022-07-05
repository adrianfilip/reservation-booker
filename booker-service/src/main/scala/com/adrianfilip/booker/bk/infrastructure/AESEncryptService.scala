package com.adrianfilip.booker.bk.infrastructure

import com.adrianfilip.booker.bk.infrastructure.EncryptionService
import zio.{Task, UIO, ULayer, ZLayer}

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Cipher, KeyGenerator}

private[infrastructure] case class AESEncryptService() extends EncryptionService:

  private def getKey(key: String): Array[Byte] = {
    val raw = MessageDigest.getInstance("MD5").digest(key.getBytes)
    raw
  }

  override def encrypt(str: String, key: String): UIO[String] =
    Task {
      val spec           = new SecretKeySpec(getKey(key), "AES")
      val plainTextBytes = str.getBytes
      val cipher         = Cipher.getInstance("AES")
      cipher.init(Cipher.ENCRYPT_MODE, spec)
      val encryptedButes = cipher.doFinal(plainTextBytes)
      val res            = Base64.getEncoder.encodeToString(encryptedButes)
      res
    }.orDie

object AESEncryptService:

  def layer: ULayer[EncryptionService] = ZLayer.succeed(AESEncryptService())
