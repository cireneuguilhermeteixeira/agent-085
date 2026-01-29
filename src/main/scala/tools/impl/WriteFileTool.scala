package tools.impl

import tools.*
import io.circe.Json

object WriteFileTool extends Tool {
  override val name = "write_file"
  override val description =
    """Writes UTF-8 text to a file under workspace (creates parent dirs).
      |Arguments JSON:
      |{ "path": "relative/path/to/file", "content": "..." }
      |""".stripMargin

  def run(arguments: Json, ctx: ToolContext): ToolResult = {
    val c = arguments.hcursor
    val pathE = c.get[String]("path")
    val contentE = c.get[String]("content")

    (pathE, contentE) match {
      case (Right(rel), Right(content)) =>
        val target = ctx.workspaceRoot / rel
        os.makeDir.all(target / os.up)
        os.write.over(target, content)
        ToolResult(true, s"Wrote ${content.length} chars to $rel")
      case _ =>
        ToolResult(false, "Missing required arguments: path, content")
    }
  }
}
