package nikalaikina.api

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import nikalaikina.api.dto._
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Uri}

import scala.language.higherKinds


trait BotAPI[F[_], S[_]] {

  def sendMessage(chatId: ChatId, message: String): F[Unit]

  def pollUpdates(fromOffset: Offset): S[BotUpdate]
}

trait StreamingBotAPI[F[_]] extends BotAPI[F, Stream[F, ?]]

class Http4SBotAPI[F[_]](
  token: String,
  client: Client[F],
  logger: Logger[F])(
  implicit
  F: Sync[F],
  D: EntityDecoder[F, BotResponse[List[BotUpdate]]]) extends StreamingBotAPI[F] {

  private val botApiUri: Uri = Uri.uri("https://api.telegram.org") / s"bot$token"

  def sendMessage(chatId: ChatId, message: String): F[Unit] = {

    val uri = botApiUri / "sendMessage" =? Map(
      "chat_id" -> List(chatId.toString),
      "parse_mode" -> List("Markdown"),
      "text" -> List(message)
    )

    client.expect[Unit](uri)
  }

  def pollUpdates(fromOffset: Offset): Stream[F, BotUpdate] =
    Stream(()).repeat.covary[F]
      .evalMapAccumulate(fromOffset) { case (offset, _) => requestUpdates(offset) }
      .flatMap { case (_, response) => Stream.emits(response.result) }

  private def requestUpdates(offset: Offset): F[(Offset, BotResponse[List[BotUpdate]])] = {

    val uri = botApiUri / "getUpdates" =? Map(
      "offset" -> List((offset + 1).toString),
      "timeout" -> List("0.5"), // timeout to throttle the polling
      "allowed_updates" -> List("""["message"]""")
    )

    client.expect[BotResponse[List[BotUpdate]]](uri)
      .map(response => (lastOffset(response).getOrElse(offset), response))
      .recoverWith {
        case ex => logger.error(ex)("Failed to poll updates").as(offset -> BotResponse(ok = true, Nil))
      }
  }

  private def lastOffset(response: BotResponse[List[BotUpdate]]): Option[Offset] =
    response.result match {
      case Nil => None
      case nonEmpty => Some(nonEmpty.maxBy(_.update_id).update_id)
    }
}
