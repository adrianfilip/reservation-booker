package com.adrianfilip.booker.ui.services

sealed trait ServiceErrors:
  def code: String
  def reason: Option[String]

object ServiceErrors:
  case class MalformedResult(status: Int, code: String = "malformed.result", reason: Option[String])
      extends ServiceErrors
  case class UnexpectedFailure(status: Int, code: String = "unexpected.error", reason: Option[String])
      extends ServiceErrors
