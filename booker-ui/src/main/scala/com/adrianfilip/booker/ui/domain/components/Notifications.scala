package com.adrianfilip.booker.ui.domain.components

import com.adrianfilip.booker.ui.domain.model.Notification
import com.raquo.laminar.api.L._

object Notifications:

  def cleanNotificationsOnEmptyInput(notifications: Var[List[Notification]]) =
    onInput.mapToValue
      .filter(_.nonEmpty)
      .map(_ => List.empty[Notification]) --> notifications.toObserver
