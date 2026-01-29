package llm

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto._

final case class ChatMessage(role: String, content: String)

final case class ToolCall(name: String, arguments: Json)

sealed trait ModelOutput
object ModelOutput {
  final case class ToolCallOut(`type`: String = "tool_call", name: String, arguments: Json) extends ModelOutput
  final case class FinalOut(`type`: String = "final", content: String) extends ModelOutput

  given Decoder[ToolCallOut] = deriveDecoder
  given Decoder[FinalOut]    = deriveDecoder

  // We decode by trying ToolCallOut, then FinalOut
  given Decoder[ModelOutput] = Decoder.instance { c =>
    c.as[ToolCallOut].orElse(c.as[FinalOut])
  }
}
