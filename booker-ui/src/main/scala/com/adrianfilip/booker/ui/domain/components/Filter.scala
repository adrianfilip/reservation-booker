package com.adrianfilip.booker.ui.domain.components

import com.raquo.laminar.api.L._

object Filter:

  def floatingDivCSS(_width: Double, _marginLeft: Double, _float: String = "left") =
    List(float(_float), width(s"${_width}%"), marginLeft(s"${_marginLeft}%"))

  def filterInputCSS(placeholderValue: String) =
    List(placeholder(placeholderValue), width("100%"))
