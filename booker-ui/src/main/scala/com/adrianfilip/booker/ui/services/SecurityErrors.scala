package com.adrianfilip.booker.ui.services

sealed trait SecurityErrors:
  def code: String
  
object SecurityErrors:
  case class Unauthorized(status: Int = 401, code: String = "unauthorized", reason: Option[String])
      extends SecurityErrors
  case class Forbidden(status: Int = 403, code: String = "unauthorized", reason: Option[String]) extends SecurityErrors
