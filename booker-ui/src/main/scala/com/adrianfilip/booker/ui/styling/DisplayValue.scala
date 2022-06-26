package com.adrianfilip.booker.ui.styling

sealed trait DisplayValue:
  def value: String

object DisplayValue:
  case object None  extends DisplayValue:
    override def value: String = "none"

  case object Block extends DisplayValue:
    override def value: String = "block"
