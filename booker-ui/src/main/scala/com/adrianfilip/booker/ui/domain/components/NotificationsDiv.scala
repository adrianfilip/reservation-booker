package com.adrianfilip.booker.ui.domain.components

import com.adrianfilip.booker.ui.domain.model.Notification
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

object NotificationsDiv:

  def apply(notifications: Var[List[Notification]]) =
    div(
      width("100%"),
      hidden <-- notifications.toObservable.map(_.isEmpty),
      children <-- notifications.toObservable.map(ls =>
        ls.map {
          case Notification.Error(message)   => div(width("100%"), color("red"), message)
          case Notification.Warning(message) => div(width("100%"), color("orange"), message)
        }
      )
    )

  implicit class RemoveAllNotifications(notifications: Var[List[Notification]]) {
    implicit def removeAll(): Unit = notifications.set(List.empty)
  }

  def notificationDivs(notificationSources: Var[List[Notification]]*): Seq[ReactiveHtmlElement[html.Div]] =
    notificationSources.map(NotificationsDiv(_))

  //todo make this effectful and then remove all custom ones like removeLoginNotifications and
  def removeAllNotifications(notificationSources: Var[List[Notification]]*): Seq[Unit] =
    notificationSources.map(_.set(List.empty))
