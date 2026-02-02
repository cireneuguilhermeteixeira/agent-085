package llm

import sttp.client3.*
import io.circe.{Encoder, Json, parser}
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
    val apiKey = sys.env.get("OPENAI_API_KEY").getOrElse("")
    if apiKey.isEmpty then
      return Left("Missing OPENAI_API_KEY env var")

    // Responses API expects `input` messages.
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

    // If API error -> show it plainly
    val jsonE = parser.parse(raw).left.map(_.message)

    jsonE.flatMap { json =>
      val errOpt = json.hcursor.downField("error").focus
      if errOpt.nonEmpty then
        Left(s"OpenAI error: ${errOpt.get.noSpaces}")
      else
        // Extract text output:
        // Responses format is structured; simplest is to use output_text helper field if present,
        // otherwise scan output array for "output_text".
        val outputText =
          json.hcursor.get[String]("output_text").toOption
            .orElse {
              // fallback: try `output[0].content[0].text`
              for
                out0 <- json.hcursor.downField("output").downN(0).focus
                text <- out0.hcursor.downField("content").downN(0).downField("text").as[String].toOption
              yield text
            }

        outputText match
          case Some(t) => Right(t)
          case None    => Left(s"Could not extract output text. Raw: $raw")
    }
