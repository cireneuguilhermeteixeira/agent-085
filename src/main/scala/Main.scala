import llm.{LlmClient, OpenAIClient, OllamaAdapter}
import tools.*
import tools.impl.*

object Main:
  def main(args: Array[String]): Unit =
    val backendName = sys.env.getOrElse("LLM_BACKEND", "openai").toLowerCase
    val model       = sys.env.getOrElse("LLM_MODEL", "gpt-4o-mini")
    val workspace   = sys.env.getOrElse("WORKSPACE_ROOT", ".")

    val ollamaBaseUrl = sys.env.getOrElse("OLLAMA_BASE_URL", "http://localhost:11434")


    val ctx = ToolContext(workspaceRoot = os.Path(workspace, os.pwd))

    val registry = ToolRegistry(
      List(
        ListFilesTool,
        ReadFileTool,
        WriteFileTool,
        HttpRequestTool
      )
    )

    val llmClient: LlmClient =
      backendName match
        case "ollama" => new OllamaAdapter(ollamaBaseUrl)
        case "openai" => OpenAIClient
        case other    => throw new RuntimeException(s"Unknown LLM_BACKEND: $other")

    val loop = agent.AgentLoop(llmClient, model, registry, ctx)

    println(s"Local Agent - backend=$backendName model=$model workspace=${ctx.workspaceRoot}")
    println("Type ':q' to quit.\n")

    while true do
      print("> ")
      val in = scala.io.StdIn.readLine()
      if in == null || in.trim == ":q" then
        println("bye"); return
      val out = loop.runOneTurn(in)
      println(s"\n$out\n")
