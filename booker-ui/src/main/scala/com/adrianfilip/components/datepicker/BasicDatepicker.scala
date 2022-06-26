package com.adrianfilip.components.datepicker

import com.raquo.laminar.api.L._

import java.time._

object BasicDatepicker:

  def datepickerDiv(selectedYear: Option[Int]) =
    div(
      width("100%"),
      div(
        float("left"),
        div(textAlign("center"), "+"),
        year(Signal.fromValue((1970 to LocalDate.now(ZoneId.systemDefault()).getYear + 5).toList), selectedYear),
        div(textAlign("center"), "-")
      ),
      div(
        float("left"),
        marginLeft("5%"),
        div(textAlign("center"), "+"),
        month(Some(9)),
        div(textAlign("center"), "-")
      ),
      div(
        float("left"),
        marginLeft("5%"),
        div(textAlign("center"), "+"),
        day(2022, 9, Some(16)),
        div(textAlign("center"), "-")
      )
    )

  def year(years: Signal[List[Int]], selectedYear: Option[Int]) =
    div(select(children <-- createOptions(years, selectedYear)))

  def month(selectedMonth: Option[Int]) =
    div(
      select(children <-- createOptions(Signal.fromValue(List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)), selectedMonth))
    )

  def day(year: Int, month: Int, selectedDay: Option[Int]) = {
    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
    val days        = Signal.fromValue((1 to daysInMonth).toList)
    div(select(children <-- createOptions(days, selectedDay)))
  }

  def createOptions(values: Signal[List[Int]], selectedValue: Option[Int]) =
    values.map(ls =>
      ls.map { y =>
        if (selectedValue.contains(y))
          option(value(y.toString), y.toString, selected(true))
        else option(value(y.toString), y.toString)
      }
    )
