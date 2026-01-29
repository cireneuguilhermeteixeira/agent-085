val scala3Version = "3.8.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Agent 085",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      // --- HTTP client (Ollama API) ---
      "com.softwaremill.sttp.client3" %% "core"  % "3.10.1",
      "com.softwaremill.sttp.client3" %% "circe" % "3.10.1",

      // --- JSON (tool calls / protocol) ---
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser"  % "0.14.10",

      // --- Filesystem utils (workspace sandbox) ---
      "com.lihaoyi" %% "os-lib" % "0.11.3",

      // --- Tests ---
      "org.scalameta" %% "munit" % "1.0.0" % Test
    )
  )
