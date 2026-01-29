# --- build stage ---
FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.1_3.3.7 AS build
WORKDIR /app
COPY . .
RUN sbt -Dsbt.color=false clean compile

# Se você estiver usando assembly, descomente:
# RUN sbt -Dsbt.color=false clean assembly

# --- run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /workspace

# Opção A: rodar via 'sbt run' (não recomendado em runtime)
# (não faça isso)

# Opção B (recomendada): copiar o jar (se usar assembly)
# Ajuste o nome do jar conforme o seu build gera
# COPY --from=build /app/target/scala-3.8.1/*assembly*.jar /usr/local/bin/agent.jar
# ENTRYPOINT ["java", "-jar", "/usr/local/bin/agent.jar"]

# Opção C (sem assembly): copiar classes + deps é mais chato; melhor usar assembly ou native-packager
