# --- build stage ---
FROM sbtscala/scala-sbt:eclipse-temurin-21_1.10.0_3.3.4 AS build
WORKDIR /app
COPY . .
RUN sbt -Dsbt.color=false clean assembly

# --- run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /workspace
COPY --from=build /app/target/scala-3.3.4/ollama-agent-scala-assembly-0.1.0.jar /usr/local/bin/agent.jar
ENV OLLAMA_BASE_URL=http://ollama:11434
ENV OLLAMA_MODEL=qwen2.5-coder
ENV WORKSPACE_ROOT=/workspace
ENTRYPOINT ["java", "-jar", "/usr/local/bin/agent.jar"]
