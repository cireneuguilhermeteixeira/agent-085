package llm

import sttp.client3.*
import sttp.client3.circe.*
import io.circe.{Encoder, parser}
import io.circe.syntax.*
import io.circe.generic.semiauto.*

final case class OpenAIResponsesRequest(
  model: String,
  input: List[Map[String, String]]
)

object OpenAIResponsesRequest:
  given Encoder[OpenAIResponsesRequest] = deriveEncoder

object OpenAIClient extends LlmClient:

  private val backend = HttpClientSyncBackend()

  override def chat(model: String, messages: List[ChatMessage]): Either[String, String] =
    val apiKey = sys.env.getOrElse("OPENAI_API_KEY", "")
    if apiKey.isEmpty then
      Left("Missing OPENAI_API_KEY env var")
    else
      val reqBody = OpenAIResponsesRequest(
        model = model,
        input = messages.map(m => Map("role" -> m.role, "content" -> m.content))
      ).asJson

      val request = basicRequest
        .post(uri"https://api.openai.com/v1/responses")
        .header("Authorization", s"Bearer $apiKey")
        .contentType("application/json")
        .body(reqBody)
        .response(asStringAlways)

      val raw = request.send(backend).body

      val parsed = for
        json <- parser.parse(raw).left.map(_.message)

        // error can be present and null
        errorCursor = json.hcursor.downField("error")
        _ <- if errorCursor.focus.exists(!_.isNull)
          then Left(errorCursor.focus.get.noSpaces)
          else Right(())

        // Return EXACTLY what the model produced in output_text.text
        // (which in your protocol is a JSON string: {"type":"final"...} or {"type":"http_request"...})
        rawText <- (
          for
            out0 <- json.hcursor.downField("output").downN(0).focus
            c0   <- out0.hcursor.downField("content").downN(0).focus
            txt  <- c0.hcursor.get[String]("text").toOption
          yield txt
        ).toRight("No output text found in response")

      yield rawText

      parsed.left.map(err => s"OpenAI error: $err\nRaw: $raw")
