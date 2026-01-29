package tools

final class ToolRegistry(tools: List[Tool]) {
  private val byName = tools.map(t => t.name -> t).toMap

  def names: List[String] = tools.map(_.name)
  def describeAll: String =
    tools.map(t => s"- ${t.name}: ${t.description}").mkString("\n")

  def get(name: String): Option[Tool] = byName.get(name)
}
