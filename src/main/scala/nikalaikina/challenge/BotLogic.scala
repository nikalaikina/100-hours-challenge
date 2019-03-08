package nikalaikina.challenge

import io.chrisdavenport.log4cats._
import cats.Monad
import cats.effect.Sync
import cats.syntax.try_._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.apply._
import cats.syntax.option._
import cats.instances.either._
import fs2._
import nikalaikina.api.{ChatId, StreamingBotAPI}
import nikalaikina.api.dto.BotMessage
import nikalaikina.challenge.BotCommand._

import scala.language.higherKinds
import scala.util.{Random, Try}

class BotLogic[F[_]: Monad](
  api: StreamingBotAPI[F],
  storage: TaskStorage[F],
  logger: Logger[F])(
  implicit F: Sync[F]) {

  /**
    * Launches the bot process
    */
  def launch: Stream[F, Unit] = pollCommands.evalMap(handleCommand)

  private def pollCommands: Stream[F, BotCommand] = for {
    update <- api.pollUpdates(0)
    command <- Stream.emits(update.message.flatMap(BotCommand.fromRawMessage).toSeq)
  } yield command

  private def handleCommand(command: BotCommand): F[Unit] = command match {
    case c: RemoveTask => removeTask(c.chatId, c.taskId)
    case c: ShowProgress => showTodoList(c.chatId)
    case c: AddEntry => addItem(c.chatId, c.task)
    case c: ShowHelp => api.sendMessage(c.chatId, List(
      "This bot stores your progress on the subjects. Commands:",
      s"`$help` - show this help message",
      s"`$show` - show current progress",
      s"`$remove` - removes task",
      s"description\ntag\nhours - adds done task",
    ).mkString("\n"))
  }

  private def removeTask(chatId: ChatId, taskId: Int): F[Unit] = for {
    _ <- storage.remove(chatId, taskId)
    _ <- logger.info(s"task $taskId is removed") *> api.sendMessage(chatId, "Task Is removed!")
  } yield ()

  private def showTodoList(chatId: ChatId): F[Unit] = for {
    items <- storage.getItems(chatId)
    msg = if (items.isEmpty) {
      "You have no tasks done!"
    } else {
      s"""
         |Your progress is:
         |
         |${FormatUtils.prettyPrintProgress(items)}
      """.stripMargin
    }
    _ <- logger.info(s"tasks queried for chat $chatId") *> api.sendMessage(chatId, msg)
  } yield ()

  private def addItem(chatId: ChatId, item: Task): F[Unit] = for {
    _ <- storage.addItem(chatId, item)
    response <- F.suspend(F.catchNonFatal(Random.shuffle(List("Ok!", "Sure!", "Noted", "Certainly!")).head))
    _ <- logger.info(s"task added for chat $chatId") *> api.sendMessage(chatId, response)
  } yield ()
}

object FormatUtils {
  def prettyPrintProgress(tasks: List[Task]): String = {
    tasks.groupBy(_.tag).map { case (tag, done) =>
        val timePassed = math.min(100, done.map(_.spent).sum / 60)
        s"`[${"▓" * timePassed}${"_" * (100 - timePassed)}]\t$tag`"
    }.mkString("\n")
  }
}

sealed trait BotCommand {
  val chatId: ChatId
}

object BotCommand {

  case class ShowHelp(chatId: ChatId) extends BotCommand
  case class RemoveTask(chatId: ChatId, taskId: Int) extends BotCommand
  case class ShowProgress(chatId: ChatId) extends BotCommand
  case class AddEntry(chatId: ChatId, task: Task) extends BotCommand

  def fromRawMessage(msg: BotMessage): Option[BotCommand] = msg.text.flatMap {
    case `help` | "/start" =>
      ShowHelp(msg.chat.id).some
    case `show` =>
      ShowProgress(msg.chat.id).some
    case `remove` =>
      RemoveTask(msg.chat.id, msg.forward_from_message_id.get).some
    case text =>
      Try {
        val description :: tag :: spentStr :: _ = text.split("\n").toList
        val spent = (spentStr.toDouble * 60).toInt
        AddEntry(
          chatId = msg.chat.id,
          task = Task(msg.message_id, msg.chat.id, tag, description, spent)
        )
      }.liftTo[Either[Throwable, ?]].toOption
  }

  val help = "?"
  val show = "/show"
  val remove = "remove"
}
