package llm

final class OllamaAdapter(baseUrl: String) extends LlmClient:
  override def chat(model: String, messages: List[ChatMessage]): Either[String, String] =
    OllamaClient.chat(baseUrl, model, messages)