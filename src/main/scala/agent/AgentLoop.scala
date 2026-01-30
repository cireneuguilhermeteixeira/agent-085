package agent

import llm.{ChatMessage, ModelOutput, LlmClient}
import llm.ModelOutput.*
import tools.*
import io.circe.parser
import io.circe.syntax.*

final class AgentLoop(
  client: LlmClient,
  model: String,
  registry: ToolRegistry,
  ctx: ToolContext
) {
  private val systemPrompt: String =
    s"""
       |You are a local coding agent. You can decide to call tools.
       |
       |AVAILABLE TOOLS:
       |${registry.describeAll}
       |
       |STRICT OUTPUT RULES:
       |- You MUST output ONLY valid JSON.
       |- If you want to use a tool, output:
       |  {"type":"tool_call","name":"<tool_name>","arguments":{...}}
       |- If you want to answer the user, output:
       |  {"type":"final","content":"..."}
       |- No markdown. No extra keys. No commentary.
       |
       |SAFETY:
       |- Prefer http_request over execute_command for HTTP tasks.
       |- Only use execute_command if explicitly needed.
       |""".stripMargin.trim

  private var history: List[ChatMessage] =
    List(ChatMessage("system", systemPrompt))

  def runOneTurn(userInput: String): String = {
    history = history :+ ChatMessage("user", userInput)

    
    val finalAnswer = stepUntilFinal(maxSteps = 12)
    history = history :+ ChatMessage("assistant", finalAnswer) // keep a plain assistant summary
    finalAnswer
  }

  private def stepUntilFinal(maxSteps: Int): String = {
    var steps = 0
    while (steps < maxSteps) {
      steps += 1

      val modelTextE = client.chat(model, history)

      modelTextE match {
        case Left(err) =>
          return s"[LLM error]\n$err"

        case Right(modelText) =>
          val parsed = parseModelOutput(modelText)
          parsed match {
            case Left(parseErr) =>
              // ask model to re-emit strict JSON
              history = history :+ ChatMessage(
                "user",
                s"""Your last output was not valid JSON for the protocol.
                   |Error: $parseErr
                   |Re-emit ONLY valid JSON in the required format.
                   |""".stripMargin
              )

            case Right(FinalOut(_, content)) =>
              return content

            case Right(ToolCallOut(_, toolName, args)) =>
              val toolOpt = registry.get(toolName)
              val result = toolOpt match {
                case None =>
                  ToolResult(false, s"Unknown tool: $toolName. Available: ${registry.names.mkString(", ")}")
                case Some(tool) =>
                  tool.run(args, ctx)
              }

              // Feed back tool result as user message (simple, works with Ollama chat format)
              val toolJson =
                Map(
                  "tool" -> toolName,
                  "ok" -> result.ok.toString,
                  "output" -> result.output
                ).asJson.noSpaces

              history = history :+ ChatMessage("user", s"TOOL_RESULT $toolJson")
          }
      }
    }
    s"Reached maxSteps=$maxSteps without a final answer."
  }

  private def parseModelOutput(raw: String): Either[String, ModelOutput] = {
    val trimmed = raw.trim
    for {
      json <- parser.parse(trimmed).left.map(_.message)
      out  <- json.as[ModelOutput].left.map(_.getMessage)
    } yield out
  }
}
