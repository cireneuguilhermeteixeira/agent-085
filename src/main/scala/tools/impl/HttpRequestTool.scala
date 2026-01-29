package tools.impl
import scala.concurrent.duration.*
import tools.*
import io.circe.Json
import sttp.client3.*
import sttp.model.Uri
import scala.util.Try

object HttpRequestTool extends Tool {
  override val name = "http_request"
  override val description =
    """Performs an HTTP request (safe alternative to curl).
      |Arguments JSON:
      |{
      |  "method": "GET|POST|PUT|PATCH|DELETE",
      |  "url": "https://example.com",
      |  "headers": { "k": "v" },      // optional
      |  "body": "string body",         // optional
      |  "timeout_ms": 15000            // optional
      |}
      |""".stripMargin

  private val backend = HttpClientSyncBackend()

  def run(arguments: Json, ctx: ToolContext): ToolResult = {
    val c = arguments.hcursor
    val method = c.get[String]("method").getOrElse("GET").toUpperCase
    val urlE = c.get[String]("url")
    val headers = c.get[Map[String, String]]("headers").getOrElse(Map.empty)
    val bodyOpt = c.get[String]("body").toOption
    val timeoutMs = c.get[Long]("timeout_ms").getOrElse(15000L)

    urlE match {
      case Left(_) => ToolResult(false, "Missing required argument: url")
      case Right(urlStr) =>
        val uriTry = Try(Uri.unsafeParse(urlStr))
        if (uriTry.isFailure) return ToolResult(false, s"Invalid url: $urlStr")

        val base = basicRequest
          .readTimeout(timeoutMs.millis)
          .headers(headers)
          .response(asStringAlways)

        val req = method match {
          case "GET"    => base.get(uriTry.get)
          case "DELETE" => base.delete(uriTry.get)
          case "POST"   => base.post(uriTry.get).body(bodyOpt.getOrElse(""))
          case "PUT"    => base.put(uriTry.get).body(bodyOpt.getOrElse(""))
          case "PATCH"  => base.patch(uriTry.get).body(bodyOpt.getOrElse(""))
          case other    => return ToolResult(false, s"Unsupported method: $other")
        }

        val resp = req.send(backend)
        val out =
          s"""status: ${resp.code}
             |headers: ${resp.headers.map(h => s"${h.name}: ${h.value}").mkString("; ")}
             |
             |body:
             |${resp.body}
             |""".stripMargin

        ToolResult(true, out)
    }
  }
}
