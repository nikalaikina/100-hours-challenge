package nikalaikina

import nikalaikina.api.UserId

package object api {
  type UserId = Long
  type Offset = Long
}

final case class BotResponse[T](ok: Boolean, result: T)

final case class Update(update_id: Long, message: Option[BotMessage], callback_query: Option[CallbackQuery])

final case class CallbackQuery(from: User, data: Option[String])

final case class BotMessage(from: User, text: Option[String])

final case class InlineKeyboardButton(text: String, callback_data: String)

final case class User(id: UserId) extends AnyVal
