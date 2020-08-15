package kz.domain.library.messages

trait Sender

case class TelegramSender(chatId: Long,
                          userId: Option[Int],
                          firstName: Option[String],
                          secondName: Option[String],
                          userName: Option[String]) extends Sender

case class HttpSender(actorPath: String) extends Sender

case class UserMessages(sender: TelegramSender,
                        message: Option[String],
                        replyTo: Option[String])

case class HttpMessages(sender: HttpSender,
                        message: Option[String],
                        replyTo: Option[String])

case class Response(sender: Sender, response: String)


