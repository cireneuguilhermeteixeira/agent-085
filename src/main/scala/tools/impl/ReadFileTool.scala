package tools.impl

import tools.*
import io.circe.Json

object ReadFileTool extends Tool {
  override val name = "read_file"
  override val description =
    """Reads a UTF-8 text file from workspace.
      |Arguments JSON:
      |{ "path": "relative/path/to/file" }
      |""".stripMargin

  def run(arguments: Json, ctx: ToolContext): ToolResult = {
    val pathE = arguments.hcursor.get[String]("path")
    pathE match {
      case Left(_) => ToolResult(false, "Missing required argument: path")
      case Right(rel) =>
        val target = ctx.workspaceRoot / rel
        if (!os.exists(target)) return ToolResult(false, s"File not found: $rel")
        if (os.isDir(target)) return ToolResult(false, s"Path is a directory: $rel")
        val content = os.read(target)
        ToolResult(true, content)
    }
  }
}
