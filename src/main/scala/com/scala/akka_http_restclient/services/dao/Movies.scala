package com.scala.akka_http_restclient.services.dao

import com.scala.akka_http_restclient.models.{ MovieIdentification, MovieInformation }
import com.scala.akka_http_restclient.services.dao.MoviesDaoDefinitions.MoviesDaoImpl
import com.scala.akka_http_restclient.utils.ActorContext
import slick.lifted.ProvenShape
import scala.concurrent.Future

private[services] trait MoviesDao {

  def create(movieInformation: MovieInformation): Future[Unit]

  def read(movieIdentification: MovieIdentification): Future[Option[MovieInformation]]

  def update(movieInformation: MovieInformation): Future[Unit]
}

object MoviesDao {
  def apply(): MoviesDao = new MoviesDaoImpl()
}

private[dao] object MoviesDaoDefinitions extends SlickDAO[MovieInformation, MovieInformation] {

  import dbProfile.profile.api._

  override lazy val query: TableQuery[MoviesTable] = TableQuery[MoviesTable]

  override def toRow(domainObject: MovieInformation): MovieInformation = ???

  override def fromRow(dbRow: MovieInformation): MovieInformation = ???

  class MoviesTable(tag: Tag) extends Table[MovieInformation](tag, "movies") {
    override def * : ProvenShape[MovieInformation] =
      (imdbId, screenId, movieTitle, availableSeats, reservedSeats) <> (MovieInformation.tupled, MovieInformation.unapply)

    def imdbId = column[String]("imdb_id")

    def screenId = column[String]("screen_id")

    def movieTitle = column[String]("movie_title")

    def availableSeats = column[Int]("available_seats")

    def reservedSeats = column[Int]("reserved_seats")

    def pk = primaryKey("movies_pk", (imdbId, screenId))

  }

  class MoviesDaoImpl extends MoviesDao with ActorContext {
    override def create(movieInformation: MovieInformation): Future[Unit] = {
      db.run(query += movieInformation).map(_ => ())
    }

    override def read(movieIdentification: MovieIdentification): Future[Option[MovieInformation]] = {
      db.run(query.filter(e => e.imdbId === movieIdentification.imdbId && e.screenId === movieIdentification.screenId).result)
        .map(_.headOption)
    }

    override def update(movieInformation: MovieInformation): Future[Unit] = {
      db.run(
        query.filter(e => e.imdbId === movieInformation.imdbId && e.screenId === movieInformation.screenId)
          .update(movieInformation)
      ).map(_ => ())
    }
  }

}
