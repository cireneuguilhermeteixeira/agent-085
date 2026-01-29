import agent.AgentLoop
import tools.*
import tools.impl.*

object Main:
  def main(args: Array[String]): Unit =
    val baseUrl = sys.env.getOrElse("OLLAMA_BASE_URL", "http://localhost:11434")
    val model   = sys.env.getOrElse("OLLAMA_MODEL", "qwen2.5-coder")
    val workspace = sys.env.getOrElse("WORKSPACE_ROOT", ".")
    val ctx = ToolContext(workspaceRoot = os.Path(workspace, os.pwd))

    val registry = ToolRegistry(
      List(
        ListFilesTool,
        ReadFileTool,
        WriteFileTool,
        HttpRequestTool,
      )
    )

    val loop = AgentLoop(baseUrl, model, registry, ctx)

    println(s"Local Agent (Scala) - Ollama baseUrl=$baseUrl model=$model workspace=${ctx.workspaceRoot}")
    println("Type ':q' to quit.\n")

    while true do
      print("> ")
      val in = scala.io.StdIn.readLine()
      if in == null || in.trim == ":q" then
        println("bye")
        return

      val out = loop.runOneTurn(in)
      println(s"\n$out\n")