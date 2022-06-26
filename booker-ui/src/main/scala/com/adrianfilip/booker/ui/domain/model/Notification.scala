package com.adrianfilip.booker.ui.domain.model

sealed trait Notification
object Notification:
  final case class Error(message: String)   extends Notification
  final case class Warning(message: String) extends Notification
