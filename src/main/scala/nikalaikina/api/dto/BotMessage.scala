package nikalaikina.api.dto

case class BotMessage(
  message_id: Long,
  chat: Chat,
  text: Option[String],
  forward_from_message_id: Option[Int])
