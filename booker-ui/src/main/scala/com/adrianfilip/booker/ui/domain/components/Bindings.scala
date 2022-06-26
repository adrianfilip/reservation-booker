package com.adrianfilip.booker.ui.domain.components

import com.adrianfilip.booker.ui.domain.model.Notification
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement

object Bindings:

  val defaultDebounceTimeMs = 500

  def bindInputToVar[T](
    associatedVar: Var[T],
    associatedNotificationsVars: Var[List[Notification]],
    inputToModel: PartialFunction[String, T],
    modelToInput: T => String,
    debounceValue: Int = defaultDebounceTimeMs
  ): Seq[Binder[ReactiveHtmlElement.Base]] =
    bindInputToVarAndNotifications(
      associatedVar = associatedVar,
      associatedNotificationsVars = associatedNotificationsVars,
      inputToModel = inputToModel,
      modelToInput = modelToInput,
      debounceValue = debounceValue,
      inputToNotifications = None
    )

  def bindInputToVarAndNotifications[T](
    associatedVar: Var[T],
    associatedNotificationsVars: Var[List[Notification]],
    inputToModel: PartialFunction[String, T],
    modelToInput: T => String,
    inputToNotifications: Option[PartialFunction[String, List[Notification]]] = None,
    debounceValue: Int = defaultDebounceTimeMs
  ): Seq[Binder[ReactiveHtmlElement.Base]] =
    List(
      composeEvents(onInput.mapToValue)(
        _.debounce(debounceValue)
          .map(v => inputToModel.lift(v))
          .filter(_.isDefined)
          .map(_.get)
      ) --> associatedVar,
      composeEvents(onInput.mapToValue)(
        _.debounce(debounceValue)
          .filter(_ => inputToNotifications.isDefined)
          .map(v => inputToNotifications.get.applyOrElse(v, (_: String) => List.empty[Notification]))
      ) --> associatedNotificationsVars,
      value <-- associatedVar.toObservable.map(modelToInput)
    )

  def bindInputToVar[T](
    associatedVar: Var[T],
    inputToModel: PartialFunction[String, T],
    modelToInput: T => String
  ): Seq[Binder[ReactiveHtmlElement.Base]] =
    bindInputToVar(associatedVar, inputToModel, modelToInput, 0)

  def bindInputToVar[T](
    associatedVar: Var[T],
    inputToModel: PartialFunction[String, T],
    modelToInput: T => String,
    debounceValue: Int
  ): Seq[Binder[ReactiveHtmlElement.Base]] =
    List(
      composeEvents(onInput.mapToValue)(
        _.debounce(debounceValue)
          .map(v => inputToModel.lift(v))
          .filter(_.isDefined)
          .map(_.get)
      ) --> associatedVar,
      value <-- associatedVar.toObservable.map(modelToInput)
    )

  def bindOnChangeToVar[T](
    associatedVar: Var[T],
    inputToModel: PartialFunction[String, T],
    modelToInput: T => String
  ): Seq[Binder[ReactiveHtmlElement.Base]] =
    bindOnChangeToVar(associatedVar, inputToModel, modelToInput, 0)

  def bindOnChangeToVar[T](
    associatedVar: Var[T],
    inputToModel: PartialFunction[String, T],
    modelToInput: T => String,
    debounceValue: Int
  ): Seq[Binder[ReactiveHtmlElement.Base]] =
    List(
      composeEvents(onChange.mapToValue)(
        _.debounce(debounceValue)
          .map(v => inputToModel.lift(v))
          .filter(_.isDefined)
          .map(_.get)
      ) --> associatedVar,
      value <-- associatedVar.toObservable.map(modelToInput)
    )
