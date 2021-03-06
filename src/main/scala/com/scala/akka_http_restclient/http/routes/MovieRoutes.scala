package com.scala.akka_http_restclient.http.routes

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive0, Route }
import com.scala.akka_http_restclient.models.{ MovieIdentification, MovieRegistration }
import com.scala.akka_http_restclient.services.MovieService
import com.scala.akka_http_restclient.utils.Protocol

class MovieRoutes(movieService: MovieService) extends Protocol {

  val movieRoutes = pathPrefix("movies" / "imdbId" / Segment / "screenId" / Segment) { (imdbId, screentId) =>
    val urlIdentifiers = MovieIdentification(imdbId, screentId)
    pathEndOrSingleSlash {
      put { //Register a movie
        entity(as[MovieRegistration]) { movieRegistration =>
          registerMovie(movieRegistration, urlIdentifiers)
        }
      } ~
        patch { //Reserve a seat at the movie
          entity(as[MovieIdentification]) { movieIdentification =>
            reserveSeat(movieIdentification, urlIdentifiers)
          }
        } ~
        get { //Retrieve information about the movie
          retrieveMovie(urlIdentifiers)
        }
    }
  }

  private def registerMovie(movieRegistration: MovieRegistration, urlIdentifiers: MovieIdentification): Route = {
    validateEquals(urlIdentifiers, movieRegistration.movieIdentification) {
      val saveResult = movieService.save(movieRegistration)
      import com.scala.akka_http_restclient.models.RegistrationResult._
      onSuccess(saveResult) {
        case RegitrationSuccessful => complete("movie registered")
        case AlreadyExists => complete(Conflict, "movie already exists")
      }
    }
  }

  private def reserveSeat(movieIdentification: MovieIdentification, urlIdentifiers: MovieIdentification): Route = {
    validateEquals(urlIdentifiers, movieIdentification) {
      val reservationResult = movieService.reserve(movieIdentification)
      import com.scala.akka_http_restclient.models.ReservationResult._
      onSuccess(reservationResult) {
        case ReservationSuccessful => complete("seat reserved")
        case NoSeatsLeft => complete(Conflict, "no seats left")
        case NoSuchMovie => notFound(movieIdentification)
      }
    }
  }

  private def retrieveMovie(urlIdentifiers: MovieIdentification): Route = {
    val movieInfo = movieService.read(urlIdentifiers)
    onSuccess(movieInfo) {
      case Some(x) => complete(x)
      case None => notFound(urlIdentifiers)
    }
  }

  private def notFound(movieIdentification: MovieIdentification): Route = {
    complete(NotFound, s"Could not find th movie identified by: $movieIdentification")
  }

  private def validateEquals(urlIdentifiers: MovieIdentification, bodyIdentifiers: MovieIdentification): Directive0 = {
    validate(urlIdentifiers == bodyIdentifiers, s"resource identifiers from the path [$urlIdentifiers] and the body: [$bodyIdentifiers] do not match")
  }

}

object MovieRoutes {
  def apply(): MovieRoutes = new MovieRoutes(MovieService())
}
