package nikalaikina.api

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.circe.{Encoder, Json}
import nikalaikina.{BotResponse, InlineKeyboardButton, Update}
import org.http4s.QueryParamEncoder.stringQueryParamEncoder
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.{EntityDecoder, QueryParamEncoder, QueryParameterValue, Uri}
import io.circe.syntax._

trait BotAPI[F[_], S[_]] {

  def sendMessage(userId: UserId, message: String, buttons: List[InlineKeyboardButton] = List.empty): F[Unit]

  def pollUpdates(fromOffset: Offset): S[Update]
}

object BotAPI {

  import io.circe._, io.circe.generic.semiauto._


  implicit val InlineKeyboardButtonEncoder: Encoder[InlineKeyboardButton] = deriveEncoder[InlineKeyboardButton]

  implicit val markupEncoder: QueryParamEncoder[List[InlineKeyboardButton]] =
    (list: List[InlineKeyboardButton]) => {
      QueryParameterValue(s"""{"inline_keyboard": [${list.asJson.noSpaces}]}""")
    }

}

trait StreamingBotAPI[F[_]] extends BotAPI[F, Stream[F, *]]

class Http4SBotAPI[F[_]](
  token: String,
  client: Client[F],
  logger: Logger[F])(
  implicit
  F: Sync[F],
  D: EntityDecoder[F, BotResponse[List[Update]]]) extends StreamingBotAPI[F] {
  import BotAPI._

  private val botApiUri: Uri = uri"https://api.telegram.org" / s"bot$token"

  def sendMessage(userId: UserId, message: String, buttons: List[InlineKeyboardButton] = List.empty): F[Unit] = {

    val uri = (botApiUri / "sendMessage" =? Map(
      "chat_id" -> List(userId.toString),
      "parse_mode" -> List("Markdown"),
      "text" -> List(message)
    )) +?? ("reply_markup", Some(buttons).filter(_.nonEmpty))

    client.expect[Unit](uri)
  }

  def pollUpdates(fromOffset: Offset): Stream[F, Update] =
    Stream(()).repeat.covary[F]
      .evalMapAccumulate(fromOffset) { case (offset, _) => requestUpdates(offset) }
      .flatMap { case (_, response) => Stream.emits(response.result) }

  private def requestUpdates(offset: Offset): F[(Offset, BotResponse[List[Update]])] = {

    val uri = botApiUri / "getUpdates" =? Map(
      "offset" -> List((offset + 1).toString),
      "timeout" -> List("0.5"), // timeout to throttle the polling
      "allowed_updates" -> List("""["message","callback_query"]""")
    )

    client.expect[BotResponse[List[Update]]](uri)
      .map(response => (lastOffset(response).getOrElse(offset), response))
      .recoverWith {
        case ex => logger.error(ex)("Failed to poll updates").as(offset -> BotResponse(ok = true, Nil))
      }
  }

  private def lastOffset(response: BotResponse[List[Update]]): Option[Offset] =
    response.result match {
      case Nil => None
      case nonEmpty => Some(nonEmpty.maxBy(_.update_id).update_id)
    }
}
