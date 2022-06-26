package com.adrianfilip.booker.ui

import Root.root
import com.raquo.laminar.api.L._
import org.scalajs.dom

object App:
  def main(args: Array[String]): Unit =
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _            = render(appContainer, root)
    }(unsafeWindowOwner)
