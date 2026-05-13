package com.splitwise.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiConfig {

    /** gemini → Google generateContent; openai → /chat/completions; groq → same path with base-url=api.groq.com */
    private String provider = "gemini";

    private boolean enabled = true;
    private String apiKey = "";

    /** OpenAI-compat model id, or Gemini model id (e.g. gemini-2.0-flash). */
    private String model = "gemini-2.0-flash";

    /** Base for OpenAI-style APIs only. Gemini uses gemini-base-url. */
    private String baseUrl = "https://api.openai.com/v1";

    /** Google Generative Language API root (Gemini REST), no trailing slash. */
    private String geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta";

    private int timeoutSeconds = 30;
    private int maxTokens = 1024;

    /**
     * Dev-only: disable TLS certificate verification for LLM HTTPS calls.
     * Never enable in production. Needed when HTTPS is intercepted by a corporate proxy (e.g. Zscaler)
     * whose root CA is not in the JVM truststore — the correct fix is to import that CA into the JVM instead.
     */
    private boolean sslInsecure = false;
}
