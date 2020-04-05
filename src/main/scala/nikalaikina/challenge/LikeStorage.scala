package nikalaikina.challenge

import cats.Monad
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.transactor.Transactor
import nikalaikina.api.UserId
import cats.effect._

class LikeStorage[F[_]: Monad: ({type L[T[_]] = Bracket[T, Throwable]})#L](db: Transactor[F]) {

  def like(userId: UserId, person: UserId, likes: Boolean): F[Unit] = {
    sql"""INSERT into likes (userId, person, likes) values ($userId, $person, $likes)
         |ON CONFLICT (userId, person)
         |DO UPDATE SET likes = $likes
         |""".stripMargin
      .update.run.transact(db).void
  }

  def get(userId: UserId): F[List[UserId]] = {
    sql"SELECT person FROM likes WHERE userId = $userId AND likes = true"
      .query[UserId].to[List].transact(db)
  }

  def getSeen(userId: UserId): F[List[UserId]] = {
    sql"SELECT person FROM likes WHERE userId = $userId"
      .query[UserId].to[List].transact(db)
  }

}
