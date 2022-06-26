package com.adrianfilip.booker.ui.domain.components

import com.raquo.airstream.core.{Observable, Signal}
import com.raquo.airstream.state.Var

object IsNot:
  extension [A](signal: Observable[A])
    def isNot(value: A): Observable[Boolean] = signal.map(_ != value)

  extension [A](varr: Var[A])
    def isNot(value: A): Observable[Boolean] = varr.toObservable.isNot(value)
