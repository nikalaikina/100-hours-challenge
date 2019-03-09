package nikalaikina.challenge

import cats.Monad
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.transactor.Transactor
import nikalaikina.api.ChatId

import scala.language.higherKinds

trait TaskStorage[F[_]] {
  def addItem(item: Task): F[Unit]
  def getItems(chatId: ChatId): F[List[Task]]
  def clearList(chatId: ChatId): F[Unit]
  def remove(chatId: ChatId, id: Long): F[Unit]
}

class PersistentTaskStorage[F[_]: Monad](db: Transactor[F]) extends TaskStorage[F] {

  def addItem(item: Task): F[Unit] = {
    import item._
    sql"INSERT into tasks (id, chatId, tag, description, spent) values ($id, $chatId, $tag, $description, $spent)"
      .update.run.transact(db).void
  }

  def getItems(chatId: ChatId): F[List[Task]] = {
    sql"SELECT id, chatId, tag, description, spent FROM tasks WHERE chatId = $chatId"
      .query[Task].to[List].transact(db)
  }

  def clearList(chatId: ChatId): F[Unit] = {
    sql"DELETE FROM tasks WHERE chatId = $chatId"
      .update.run.transact(db).void
  }

  def remove(chatId: ChatId, id: Long): F[Unit] = {
    sql"DELETE FROM tasks WHERE chatId = $chatId AND id = $id"
      .update.run.transact(db).void
  }
}

case class Task(id: Long, chatId: ChatId, tag: String, description: String, spent: Int)
