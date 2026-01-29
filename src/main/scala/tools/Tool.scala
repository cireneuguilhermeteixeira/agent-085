package tools

import io.circe.Json

final case class ToolResult(ok: Boolean, output: String)

trait Tool {
  def name: String
  def description: String
  def run(arguments: Json, ctx: ToolContext): ToolResult
}

final case class ToolContext(
  workspaceRoot: os.Path
)
