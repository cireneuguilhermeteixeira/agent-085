package tools.impl

import tools.*
import io.circe.Json

object ListFilesTool extends Tool {
  override val name = "list_files"
  override val description =
    """Lists files under a relative path.
      |Arguments JSON:
      |{ "path": "relative/path" }  // optional, defaults to "."
      |""".stripMargin

  def run(arguments: Json, ctx: ToolContext): ToolResult = {
    val rel = arguments.hcursor.get[String]("path").getOrElse(".")
    val target = ctx.workspaceRoot / rel

    if (!os.exists(target)) return ToolResult(false, s"Path not found: $rel")
    if (!os.isDir(target)) return ToolResult(false, s"Not a directory: $rel")

    val entries = os.list(target).map { p =>
      val kind = if (os.isDir(p)) "dir" else "file"
      s"$kind\t${p.relativeTo(ctx.workspaceRoot)}"
    }
    ToolResult(true, entries.mkString("\n"))
  }
}
