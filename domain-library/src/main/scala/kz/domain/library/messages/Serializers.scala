package kz.domain.library.messages

import org.json4s.ShortTypeHints
import org.json4s.native.Serialization

trait Serializers {
  implicit val formats = Serialization.formats(ShortTypeHints(List(classOf[TelegramSender], classOf[HttpSender])))
}