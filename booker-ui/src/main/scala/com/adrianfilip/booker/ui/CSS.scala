package com.adrianfilip.booker.ui

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object CSS:
  @js.native
  @JSImport("stylesheets/bootstrap.css", JSImport.Namespace)
  object Bootstrap4CSS extends js.Any

  @js.native
  @JSImport("stylesheets/locations.css", JSImport.Namespace)
  object LocationsCSS extends js.Any

  @js.native
  @JSImport("stylesheets/root.css", JSImport.Namespace)
  object RootCSS extends js.Any

  @js.native
  @JSImport("stylesheets/cellModal.scss", JSImport.Namespace)
  object CellModalCSS extends js.Any

  @js.native
  @JSImport("stylesheets/datepicker.scss", JSImport.Namespace)
  object DatepickerCSS extends js.Any
