trait LlmClient:
  def chat(model: String, messages: List[ChatMessage]): Either[String, String]