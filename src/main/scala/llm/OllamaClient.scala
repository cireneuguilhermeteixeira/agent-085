package llm

import sttp.client3.*
import sttp.client3.circe.*
import io.circe.{Encoder, Json, parser}
import io.circe.syntax.*
import io.circe.generic.semiauto.*
import java.net.http.HttpClient
import java.time.Duration

final case class OllamaChatRequest(
  model: String,
  messages: List[Map[String, String]],
  stream: Boolean
)

object OllamaChatRequest:
  given Encoder[OllamaChatRequest] = deriveEncoder

object OllamaClient:
  
  private val httpClient: HttpClient =
    HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(0))
      .build()

  private val backend = HttpClientSyncBackend.usingClient(httpClient)

  def chat(
    baseUrl: String,
    model: String,
    messages: List[ChatMessage]
  ): Either[String, String] =
    val reqBody = OllamaChatRequest(
      model = model,
      messages = messages.map(m => Map("role" -> m.role, "content" -> m.content)),
      stream = false
    ).asJson

    val request = basicRequest
      .post(uri"$baseUrl/api/chat")
      .contentType("application/json")
      .body(reqBody)
      .response(asStringAlways)

    val resp = request.send(backend).body

    val decoded = for
      json <- parser.parse(resp).left.map(_.message)
      msg  <- json.hcursor.downField("message").as[Json].left.map(_.message)
      content <- msg.hcursor.downField("content").as[String].left.map(_.message)
    yield content

    decoded.left.map(err => s"Ollama response parse error: $err\nRaw: $resp")
