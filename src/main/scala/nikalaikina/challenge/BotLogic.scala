package nikalaikina.challenge

import cats.Monad
import cats.data.Nested
import cats.effect.Sync
import cats.instances.either._
import cats.instances.list._
import cats.instances.option._
import cats.syntax.applicative._
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import cats.syntax.traverse._
import cats.syntax.try_._
import io.chrisdavenport.log4cats._
import fs2._
import nikalaikina.api.{StreamingBotAPI, UserId}
import nikalaikina.challenge.BotCommand._
import nikalaikina.{BotMessage, CallbackQuery, InlineKeyboardButton, Update}

import scala.util.{Random, Try}

class BotLogic[F[_]: Monad](
                             api: StreamingBotAPI[F],
                             people: PeopleStorage[F],
                             likes: LikeStorage[F],
                             logger: Logger[F])(
  implicit F: Sync[F]) {

  def launch: Stream[F, Unit] = pollCommands.evalMap(handleCommand)

  private def pollCommands: Stream[F, BotCommand] = for {
    update <- api.pollUpdates(0)
    command <- Stream.emits(BotCommand.fromRawMessage(update).toSeq)
  } yield command

  private def handleCommand(command: BotCommand): F[Unit] = {
    implicit val userId: UserId = command.userId
    command match {
      case c: AddEntry => addItem(c.person) *> sendPerson
      case c: Like => like(c) *> checkLike(c) *> sendPerson
      case c: ShowHelp => api.sendMessage(c.userId, List(
        "This bot stores your progress on the subjects. Commands:",
        s"`$help` - show this help message",
        s"description\ncontact - starts the game",
      ).mkString("\n"))
    }
  }

  private def addItem(item: Person): F[Unit] = people.add(item)

  private def like(like: Like): F[Unit] = likes.like(like.userId, like.person, like.like)

  private def sendPerson(implicit userId: UserId): F[Unit] = for {
    allPeople <- people.getAll
    seen <- likes.getSeen(userId)
    people = allPeople.filter(p => p.userId != userId && !seen.contains(p.userId))
    person = Random.shuffle(people).headOption
    msg = person.fold("No people left")(_.description)
    buttons = person.map(person => List(
      InlineKeyboardButton("Like", s"like ${person.userId}"),
      InlineKeyboardButton("Dislike", s"dislike ${person.userId}")
    )).orEmpty
    _ <- api.sendMessage(userId, msg, buttons)
  } yield ()

  private def checkLike(like: Like): F[Unit] = for {
    likes <- likes.get(like.person)
    mutual = likes.contains(like.userId)
    _ <- (sendMatch(like.userId, like.person) *> sendMatch(like.person, like.userId)).whenA(mutual)
  } yield ()

  private def sendMatch(userId: UserId, person: UserId) = {
    matchMsg(userId).flatMap(_.traverse(api.sendMessage(person, _)))
  }

  private def matchMsg(about: UserId): F[Option[String]] = {
    Nested(people.get(about)).map { person =>
      s"""
         |You got match!
         |
         |${person.description}
         |
         |Contact: ${person.contact}
         |""".stripMargin
    }.value
  }
}

sealed trait BotCommand {
  val userId: UserId
}

object BotCommand {

  case class ShowHelp(userId: UserId) extends BotCommand
  case class AddEntry(userId: UserId, person: Person) extends BotCommand
  case class Like(userId: UserId, person: UserId, like: Boolean) extends BotCommand

  def fromRawMessage(msg: Update): Option[BotCommand] = {
    def textCommand = msg.message flatMap {
      case BotMessage(user, Some(`help` | `start`)) =>
        ShowHelp(user.id).some
      case BotMessage(user, Some(text)) =>
        Try {
          val description :: contact :: _ = text.split("\n").toList
          AddEntry(user.id, Person(user.id, description, contact))
        }.liftTo[Either[Throwable, *]].toOption
      case _ => None
    }

    def callbackCommand = msg.callback_query.collect {
      case CallbackQuery(from, Some(data)) =>
        data.split(" ").toList match {
          case like :: userId :: Nil => Some(Like(from.id, userId.toLong, like == "like"))
          case _ => None
        }
    }.flatten
    callbackCommand orElse textCommand
  }

  val help = "?"
  val start = "/start"
}
