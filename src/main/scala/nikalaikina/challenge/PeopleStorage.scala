package nikalaikina.challenge

import cats.Monad
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.transactor.Transactor
import nikalaikina.api.UserId
import cats.effect._

class PeopleStorage[F[_]: Monad: ({type L[T[_]] = Bracket[T, Throwable]})#L](db: Transactor[F]) {

  def add(item: Person): F[Unit] = {
    import item._
    sql"""INSERT into people (userId, description, contact) values ($userId, $description, $contact)
         |ON CONFLICT (userId)
         |DO UPDATE SET description = $description, contact = $contact
         |""".stripMargin
      .update.run.transact(db).void
  }

  def get(userId: UserId): F[Option[Person]] = {
    sql"SELECT userId, description, contact FROM people WHERE userId = $userId"
      .query[Person].to[List].transact(db).map(_.headOption)
  }


  val getAll: F[List[Person]] = {
    sql"SELECT userId, description, contact FROM people"
      .query[Person].to[List].transact(db)
  }

}

case class Person(userId: UserId, description: String, contact: String)
