package com.splitwise.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitwise.ai.AiConfig;
import com.splitwise.exception.UpstreamAiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    private HttpClient llmHttpClient;

    public String complete(String systemPrompt, String userMessage) {
        if (!aiConfig.isEnabled() || aiConfig.getApiKey().isBlank()) {
            throw new AiDisabledException("AI features are disabled. Set AI_API_KEY and AI_ENABLED=true.");
        }
        try {
            if (isGemini()) {
                return completeGemini(systemPrompt, userMessage, false);
            }
            return completeOpenAi(systemPrompt, userMessage, false);
        } catch (AiDisabledException e) {
            throw e;
        } catch (UpstreamAiException e) {
            throw e;
        } catch (Exception e) {
            throw rewriteLlmException(e);
        }
    }

    public String completeWithJsonMode(String systemPrompt, String userMessage) {
        if (!aiConfig.isEnabled() || aiConfig.getApiKey().isBlank()) {
            throw new AiDisabledException("AI features are disabled. Set AI_API_KEY and AI_ENABLED=true.");
        }
        try {
            if (isGemini()) {
                return completeGemini(systemPrompt, userMessage, true);
            }
            return completeOpenAi(systemPrompt, userMessage, true);
        } catch (AiDisabledException e) {
            throw e;
        } catch (UpstreamAiException e) {
            throw e;
        } catch (Exception e) {
            throw rewriteLlmException(e);
        }
    }

    private boolean isGemini() {
        return aiConfig.getProvider() != null
                && "gemini".equalsIgnoreCase(aiConfig.getProvider().trim());
    }

    private synchronized HttpClient httpClient() {
        if (llmHttpClient == null) {
            HttpClient.Builder b = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()));
            if (aiConfig.isSslInsecure()) {
                log.warn("app.ai.ssl-insecure=true: TLS verification is DISABLED for LLM HTTP client. Never use this in production.");
                try {
                    b.sslContext(sslContextTrustAll());
                } catch (Exception e) {
                    throw new IllegalStateException("Could not configure insecure SSL for LLM client", e);
                }
            }
            llmHttpClient = b.build();
        }
        return llmHttpClient;
    }

    /** Only for ssl-insecure / local debugging — not for production. */
    private static SSLContext sslContextTrustAll() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustAll, new SecureRandom());
        return ctx;
    }

    private RuntimeException rewriteLlmException(Exception e) {
        if (findSslHandshakeFailure(e) != null) {
            log.error("LLM TLS handshake failed (often corporate SSL inspection / missing proxy root in JVM truststore)", e);
            String hint = "TLS certificate verification failed (PKIX). Common when a corporate proxy (e.g. Zscaler) re-signs HTTPS: "
                    + "either import your organization's root CA into the JVM truststore (correct fix), or for local debugging only set "
                    + "AI_SSL_INSECURE=true in backend/.env and restart.";
            if (!aiConfig.isSslInsecure()) {
                throw new UpstreamAiException(HttpStatus.BAD_GATEWAY, hint);
            }
        }
        log.error("LLM call failed", e);
        return new RuntimeException("Failed to get AI response: " + e.getMessage(), e);
    }

    /** Walk cause chain for SSL handshake / PKIX wording. */
    private static Throwable findSslHandshakeFailure(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SSLHandshakeException) {
                return t;
            }
            String msg = t.getMessage();
            if (msg != null && (msg.contains("PKIX path building failed") || msg.contains("certificate_unknown"))) {
                return t;
            }
        }
        return null;
    }

    /** OpenAI or any host exposing POST {base}/chat/completions + Bearer api key. */
    private String completeOpenAi(String systemPrompt, String userMessage, boolean jsonMode) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiConfig.getModel());
        body.put("max_tokens", aiConfig.getMaxTokens());
        body.put("temperature", jsonMode ? 0.1 : 0.3);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        );
        body.put("messages", messages);
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiConfig.getApiKey())
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return parseOpenAiCompletion(response);
    }

    private String parseOpenAiCompletion(HttpResponse<String> response) throws Exception {
        if (response.statusCode() != 200) {
            log.error("LLM API error: status={}, body={}", response.statusCode(), response.body());
            String detail = extractOpenAiStyleError(response.body()).orElse("status " + response.statusCode());
            throw mapOpenAiUpstream(response.statusCode(), detail);
        }
        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    /** Gemini Generative Language API: POST .../models/{model}:generateContent?key= */
    private String completeGemini(String systemPrompt, String userMessage, boolean jsonMime) throws Exception {
        String modelId = aiConfig.getModel().trim();
        String key = URLEncoder.encode(aiConfig.getApiKey(), StandardCharsets.UTF_8).replace("+", "%20");
        String uri = aiConfig.getGeminiBaseUrl().replaceAll("/+$", "") + "/models/" + modelId + ":generateContent?key="
                + key;

        Map<String, Object> systemInstr = Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        );

        Map<String, Object> userTurn = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        );

        Map<String, Object> generation = new LinkedHashMap<>();
        generation.put("temperature", jsonMime ? 0.1 : 0.3);
        generation.put("maxOutputTokens", aiConfig.getMaxTokens());
        if (jsonMime) {
            generation.put("responseMimeType", "application/json");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("systemInstruction", systemInstr);
        payload.put("contents", List.of(userTurn));
        payload.put("generationConfig", generation);

        String json = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return parseGeminiResponse(response);
    }

    private String parseGeminiResponse(HttpResponse<String> response) throws Exception {
        String raw = response.body();
        if (response.statusCode() != 200) {
            log.error("Gemini API error: status={}, body={}", response.statusCode(), raw);
            String detail = extractGeminiStyleError(raw).orElse(raw);
            throw mapGeminiUpstream(response.statusCode(), detail);
        }

        JsonNode root = objectMapper.readTree(raw);
        if (root.has("error")) {
            JsonNode err = root.get("error");
            String msg = err.path("message").asText(err.toString());
            throw mapGeminiUpstream(err.path("code").asInt(400), msg);
        }

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.size() == 0) {
            throw new RuntimeException("Gemini returned no candidates: " + raw);
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.size() == 0) {
            throw new RuntimeException("Gemini returned empty content: " + raw);
        }

        StringBuilder sb = new StringBuilder();
        for (JsonNode p : parts) {
            JsonNode txt = p.get("text");
            if (txt != null && txt.isTextual()) {
                sb.append(txt.asText());
            }
        }
        String out = sb.toString().trim();
        if (out.isBlank()) {
            throw new RuntimeException("Gemini returned blank text");
        }

        JsonNode fb = candidates.get(0).path("finishReason");
        if (fb.isTextual() && "SAFETY".equalsIgnoreCase(fb.asText())) {
            throw new RuntimeException("Gemini blocked response (safety).");
        }

        return out;
    }

    private Optional<String> extractGeminiStyleError(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode err = root.get("error");
            if (err != null && !err.isMissingNode()) {
                return Optional.ofNullable(err.path("message").asText(null));
            }
        } catch (Exception ignored) {
            // ignore
        }
        return Optional.empty();
    }

    private Optional<String> extractOpenAiStyleError(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode err = root.path("error");
            if (!err.isMissingNode()) {
                if (err.isTextual()) {
                    return Optional.ofNullable(err.asText());
                }
                return Optional.ofNullable(err.path("message").asText(null));
            }
        } catch (Exception ignored) {
            // ignore
        }
        return Optional.empty();
    }

    private UpstreamAiException mapGeminiUpstream(int statusCode, String detail) {
        String d = detail == null ? "" : detail.trim();
        if (statusCode == 429 || d.contains("RESOURCE_EXHAUSTED") || d.contains("exceeded your current quota")) {
            String msg = "Gemini quota or rate limit was exceeded (often free-tier limits per model). "
                    + "Check https://ai.google.dev/gemini-api/docs/rate-limits and billing; "
                    + "you can try AI_MODEL=gemini-2.5-flash or gemini-1.5-flash if your tier allows.";
            return new UpstreamAiException(HttpStatus.TOO_MANY_REQUESTS, msg);
        }
        if (statusCode == 400 && (d.contains("API key not valid") || d.contains("API_KEY_INVALID"))) {
            return new UpstreamAiException(HttpStatus.BAD_REQUEST,
                    "Gemini rejected the API key (API_KEY_INVALID). Copy a fresh key from Google AI Studio into backend/.env as AI_API_KEY, restart the server, and confirm the key project has Gemini API enabled.");
        }
        if (statusCode >= 400 && statusCode < 500) {
            return new UpstreamAiException(HttpStatus.BAD_REQUEST,
                    "Gemini rejected the request: " + shorten(d, 500));
        }
        return new UpstreamAiException(HttpStatus.BAD_GATEWAY,
                "Gemini upstream error (" + statusCode + "): " + shorten(d, 500));
    }

    private UpstreamAiException mapOpenAiUpstream(int statusCode, String detail) {
        String d = detail == null ? "" : detail.trim();
        if (statusCode == 429) {
            return new UpstreamAiException(HttpStatus.TOO_MANY_REQUESTS,
                    "LLM rate limit or quota exceeded. Retry later or upgrade your provider plan.");
        }
        if (statusCode >= 400 && statusCode < 500) {
            return new UpstreamAiException(HttpStatus.BAD_REQUEST,
                    "LLM provider rejected the request: " + shorten(d, 500));
        }
        return new UpstreamAiException(HttpStatus.BAD_GATEWAY,
                "LLM upstream error (" + statusCode + "): " + shorten(d, 500));
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
