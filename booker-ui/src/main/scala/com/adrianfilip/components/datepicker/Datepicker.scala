package com.adrianfilip.components.datepicker

import com.adrianfilip.booker.ui.CSS.{CellModalCSS, DatepickerCSS}
import com.adrianfilip.booker.ui.styling.DisplayValue
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.TableCell

import java.time._
import java.util.UUID
import scala.util.Try

import com.adrianfilip.components.datepicker.Datepicker.*

case class Datepicker(selectedDay: Var[Day], showModal: Var[DisplayValue]):

  val cellModalCSS: CellModalCSS.type   = CellModalCSS
  val datepickerCSS: DatepickerCSS.type = DatepickerCSS

  private val mouseOverDay: EventBus[Option[Int]]                               = new EventBus()
  private val componentEventBus: EventBus[ChangeDay | ChangeMonth | ChangeYear] = new EventBus()

  private def maxSupportedDay(year: Int, month: Int, day: Int): Int =
    if (day <= 28) day
    else if (Try(LocalDate.of(year, month, day)).isSuccess) day
    else maxSupportedDay(year, month, day - 1)

  private val handleComponentEvents =
    List(
      componentEventBus.events.collect { case ChangeDay(day) => day } --> { day =>
        println(s"ChangeDay: $day")
        selectedDay.set(selectedDay.now().copy(day = day))
        showModal.set(DisplayValue.None)
      },
      componentEventBus.events.collect { case ChangeMonth(month) => month } --> { month =>
        println(s"ChangeMonth: $month")
        val now = selectedDay.now()
        Try(LocalDate.of(now.year, month, now.day)) match {
          case scala.util.Success(_) =>
            selectedDay.set(now.copy(month = month))
          case scala.util.Failure(_) =>
            selectedDay.set(now.copy(month = month, day = maxSupportedDay(now.year, month, now.day)))
        }
      },
      componentEventBus.events.collect { case ChangeYear(year) => year } --> { year =>
        println(s"ChangeYear: $year")
        val now = selectedDay.now()
        Try(LocalDate.of(year, now.month, now.day)) match {
          case scala.util.Success(_) =>
            selectedDay.set(now.copy(year = year))
          case scala.util.Failure(_) =>
            selectedDay.set(now.copy(year = year, day = maxSupportedDay(year, now.month, now.day)))
        }
      }
    )

  def makeModal = {
    val modalDivId = UUID.randomUUID().toString
    div(
      idAttr(modalDivId),
      float("left"),
      className("cellModal"),
      display <-- showModal.signal.map(_.value),
      width("100%"),
      //when I click outside the modal content I hide the modal
      onClick.filter(_.target.id == modalDivId).map(_ => DisplayValue.None) --> showModal,
      children <-- selectedDay.signal.map(_ =>
        Seq(
          div(
            className("cell-modal-content"),
            width("50%"),
            marginLeft("25%"),
            span("X", className("cell-modal-close"), onClick.map(_ => DisplayValue.None) --> showModal),
            div(height("5%"), width("100%")),
            //year & month
            div(
              width("100%"),
              div(
                width("100%"),
                div("Year:", width("10%"), float("left")),
                div(
                  float("left"),
                  width("20%"),
                  select(
                    children <-- createOptions(
                      Signal.fromValue((1970 to LocalDate.now(ZoneId.systemDefault()).getYear + 5).toList),
                      Some(selectedDay.now().year)
                    ),
                    onChange.mapToValue.map(v => ChangeYear(v.toInt)) --> componentEventBus
                  )
                ),
                div(width("10%"), float("left")),
                div(float("left"), "Month:", width("10%")),
                div(
                  width("50%"),
                  float("left"),
                  select(
                    children <-- createOptions(Signal.fromValue((1 to 12).toList), Some(selectedDay.now().month)),
                    onChange.mapToValue.map(v => ChangeMonth(v.toInt)) --> componentEventBus
                  )
                )
              ),
              div(height("10%"), width("100%")),
              //days
              div(
                width("100%"),
                table(
                  width("100%"),
                  className("table"),
                  thead(
                    width("100%"),
                    className("thead"),
                    children <-- Signal
                      .fromValue(List("M", "T", "W", "T", "F", "S", "S"))
                      .map(vs => vs.map(v => th(v, textAlign("center"))))
                  ),
                  tbody(children <-- Signal.fromValue(createRows))
                )
              )
            )
          )
        )
      ),
      handleComponentEvents
    )
  }

  private def createOptions(values: Signal[List[Int]], selectedValue: Option[Int]) =
    values.map(ls =>
      ls.map { y =>
        if (selectedValue.contains(y))
          option(value(y.toString), y.toString, selected(true))
        else option(value(y.toString), y.toString)
      }
    )

  private def createRows = {
    val yearMonth         = YearMonth.of(selectedDay.now().year, selectedDay.now().month)
    val daysInMonth       = yearMonth.lengthOfMonth
    val year              = yearMonth.getYear
    val month             = yearMonth.getMonthValue
    val daysWithDayOfWeek = (1 to daysInMonth).map(day => (day, LocalDate.of(year, month, day).getDayOfWeek)).toList
    val firstSunday       = daysWithDayOfWeek.find { case (_, dow) => dow == DayOfWeek.SUNDAY }.map(_._1).getOrElse(0)
    val daysInFirstWeek   = daysWithDayOfWeek.slice(0, firstSunday)
    val otherWeeks        = daysWithDayOfWeek.slice(firstSunday, daysWithDayOfWeek.length).grouped(7).toList
    val allWeeks          = List(daysInFirstWeek) ++ otherWeeks
    createTds(allWeeks).map(tds => tr(children <-- Signal.fromValue(tds)))
  }

  private def createTds(ls: List[List[(Int, DayOfWeek)]]): List[List[ReactiveHtmlElement[TableCell]]] =
    ls match {
      case Nil            => List.empty
      case ::(head, Nil)  => List(padBack(createCells(head)))
      case ::(head, next) =>
        List(padFront(createCells(head))) ++ createTds(next)
    }

  private def createCells(ls: List[(Int, DayOfWeek)]) =
    ls.map { case (dayNo, _) =>
      td(
        dayNo.toString,
        textAlign("center"),
        className <-- selectedDay.signal.map(sd => dayNo == sd.day).map(s => if (s) "datepickerSelectedCell" else ""),
        className <-- mouseOverDay.events
          .map(_.contains(dayNo) && selectedDay.now().day != dayNo)
          .map(s => if (s) "datepickerMouseOverCell" else ""),
        onMouseOver.map(_ => Some(dayNo)) --> mouseOverDay,
        onMouseOut.map(_ => None) --> mouseOverDay,
        onClick.map(_ => ChangeDay(dayNo)) --> componentEventBus
      )
    }

  private def padFront(ls: List[ReactiveHtmlElement[TableCell]]) =
    createPads(ls.size) ++ ls

  private def padBack(ls: List[ReactiveHtmlElement[TableCell]]) =
    ls ++ createPads(ls.size)

  private def createPads(days: Int) =
    if (days == 7) List.empty
    else (1 to (7 - days)).toList.map(_ => td())

object Datepicker:
  case class Day(year: Int, month: Int, day: Int)
  object Day {
    def from(now: LocalDate): Day = Day(now.getYear, now.getMonthValue, now.getDayOfMonth)
  }

  case class ChangeDay(day: Int)
  case class ChangeMonth(month: Int)
  case class ChangeYear(year: Int)
