package nikalaikina.challenge

import cats.Functor
import cats.effect.concurrent.Ref
import cats.implicits._
import nikalaikina.api.ChatId

import scala.language.higherKinds

trait TaskStorage[F[_]] {
  def addItem(chatId: ChatId, item: Task): F[Unit]
  def getItems(chatId: ChatId): F[List[Task]]
  def clearList(chatId: ChatId): F[Unit]
  def remove(chatId: ChatId, id: Long): F[Unit]
}

class InMemoryTaskStorage[F[_] : Functor](
  private val ref: Ref[F, Map[ChatId, List[Task]]]) extends TaskStorage[F] {

  def addItem(chatId: ChatId, item: Task): F[Unit] =
    ref.update(m => m.updated(chatId, item :: m.getOrElse(chatId, Nil))).void

  def getItems(chatId: ChatId): F[List[Task]] =
    ref.get.map(_.getOrElse(chatId, Nil))

  def clearList(chatId: ChatId): F[Unit] =
    ref.update(_ - chatId).void

  def remove(chatId: ChatId, id: Long): F[Unit] =
    ref.update(m => m.updated(chatId, m.getOrElse(chatId, Nil).filter(_.id != chatId))).void
}

case class Task(id: Long, chatId: ChatId, tag: String, description: String, spent: Int)
