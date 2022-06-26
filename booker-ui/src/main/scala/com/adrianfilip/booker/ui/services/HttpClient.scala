package com.adrianfilip.booker.ui.services

import com.adrianfilip.booker.scaleaware.appresponse.FailedResponse
import com.raquo.airstream.core.EventStream
import io.laminext.fetch.{Fetch, FetchEventStreamBuilder, FetchResponse}
import zio.ZIO
import zio.json.*

import scala.util.{Failure, Success}

object HttpClient:
  def handleResponse[A](res: FetchResponse[String])(implicit jsonDecoder: JsonDecoder[A]): A | ServiceErrors |
    SecurityErrors                                                                                               =
    res.status match
      case 401 =>
        res.data.fromJson[FailedResponse] match
          case Left(_)      => SecurityErrors.Unauthorized(reason = Some(res.data))
          case Right(value) => SecurityErrors.Unauthorized(code = value.code, reason = value.details)
      case 403 =>
        res.data.fromJson[FailedResponse] match
          case Left(_)      => SecurityErrors.Forbidden(reason = Some(res.data))
          case Right(value) => SecurityErrors.Forbidden(code = value.code, reason = value.details)
      case 200 =>
        res.data.fromJson[A] match
          case Right(value) => value
          case Left(value)  => ServiceErrors.MalformedResult(status = res.status, reason = Some(value))
      case 500 =>
        res.data.fromJson[FailedResponse] match
          case Left(value)  => ServiceErrors.MalformedResult(status = res.status, reason = Some(value))
          case Right(value) =>
            ServiceErrors.MalformedResult(status = res.status, code = value.code, reason = value.details)
      case _   => ServiceErrors.MalformedResult(status = res.status, reason = Some(res.data))

  def handleResponse[A, B](
    res: FetchResponse[String]
  )(implicit jsonDecoderA: JsonDecoder[A], jsonDecoderB: JsonDecoder[B]): A | B | ServiceErrors | SecurityErrors =
    res.status match
      case 401 =>
        res.data.fromJson[FailedResponse] match
          case Left(_)      => SecurityErrors.Unauthorized(reason = Some(res.data))
          case Right(value) => SecurityErrors.Unauthorized(code = value.code, reason = value.details)
      case 403 =>
        res.data.fromJson[FailedResponse] match
          case Left(_)      => SecurityErrors.Forbidden(reason = Some(res.data))
          case Right(value) => SecurityErrors.Forbidden(code = value.code, reason = value.details)
      case 200 =>
        res.data.fromJson[A] match
          case Right(value) => value
          case Left(value)  => ServiceErrors.MalformedResult(status = res.status, reason = Some(value))
      case _   =>
        res.data.fromJson[B] match
          case Right(value) => value
          case Left(value)  =>
            res.status match
              case 500 =>
                res.data.fromJson[FailedResponse] match
                  case Left(_)      => ServiceErrors.MalformedResult(status = res.status, reason = Some(value))
                  case Right(value) =>
                    ServiceErrors.MalformedResult(status = res.status, code = value.code, reason = value.details)
              case _   => ServiceErrors.MalformedResult(status = res.status, reason = Some(value))

  def catchServiceErrors(err: Throwable): ZIO[Any, Nothing, EventStream[ServiceErrors.UnexpectedFailure]]        =
    ZIO.succeed(
      EventStream
        .fromValue(ServiceErrors.UnexpectedFailure(status = 500, reason = Some("Unexpected error:" + err.getMessage)))
    )

  def makeRequest[A, B <: A, E <: A](req: FetchEventStreamBuilder)(implicit
    jsonDecoderB: JsonDecoder[B],
    jsonDecoderE: JsonDecoder[E]
  ): ZIO[Any, Nothing, EventStream[A | ServiceErrors | SecurityErrors]] =
    ZIO
      .attempt {
        req.text
          .map { response =>
            println("FETCH RESPONSE multi:" + response)
            handleResponse[B, E](response)
          }
          .recoverToTry
          .map[A | ServiceErrors | SecurityErrors] { tr =>
            tr match
              case Failure(exception) =>
                ServiceErrors.UnexpectedFailure(500, "Failed backend call", Some(exception.getMessage))
              case Success(value)     => value
          }
          .map { v =>
            println("FETCH RESPONSE unraw:" + v)
            v
          }
      }
      .catchAll(catchServiceErrors)

  def makeRequest[A](
    req: FetchEventStreamBuilder
  )(implicit jsonDecoderA: JsonDecoder[A]): ZIO[Any, Nothing, EventStream[A | ServiceErrors | SecurityErrors]] =
    ZIO
      .attempt {
        req.text
          .map { response =>
            println("FETCH RESPONSE single:" + response)
            handleResponse[A](response)
          }
          .recoverToTry
          .map[A | ServiceErrors | SecurityErrors] { tr =>
            tr match
              case Failure(exception) =>
                ServiceErrors.UnexpectedFailure(500, "Failed backend call", Some(exception.getMessage))
              case Success(value)     => value
          }
          .map { v =>
            println("FETCH RESPONSE unraw:" + v)
            v
          }
      }
      .catchAll(catchServiceErrors)

