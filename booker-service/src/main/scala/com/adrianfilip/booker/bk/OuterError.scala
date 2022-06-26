package com.adrianfilip.booker.bk

import com.adrianfilip.booker.scaleaware.appresponse.FailedResponse
import zhttp.http.{HttpData, Response, Status}
import zio.json.*

sealed trait OuterError extends Throwable

object OuterError:
  final case class BadRequestError(message: String)           extends OuterError
  final case class MissingAuthorizationToken(message: String) extends OuterError

  extension (error: OuterError)
    def toResponse = error match {
      case OuterError.BadRequestError(message)           =>
        Response.apply(
          status = Status.BadRequest,
          data = HttpData.fromString(FailedResponse("bad.request", Some(message)).toJson)
        )
      case OuterError.MissingAuthorizationToken(message) =>
        Response.apply(
          status = Status.Unauthorized,
          data = HttpData.fromString(FailedResponse("missing.token", Some(message)).toJson)
        )
    }
