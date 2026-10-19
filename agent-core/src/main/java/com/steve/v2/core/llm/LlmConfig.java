package com.steve.v2.core.llm;

import java.net.URI;

public record LlmConfig(Provider provider, URI endpoint, String apiKey, String model,
                        int maxTokens, double temperature) {
    public enum Provider { OPENAI, GROQ, GEMINI }
    public LlmConfig {
        if (provider == null || endpoint == null || !"https".equals(endpoint.getScheme()))
            throw new IllegalArgumentException("An HTTPS AI endpoint is required");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("Configure the selected provider's API key");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("Configure the selected provider's model");
        if (maxTokens < 128 || maxTokens > 16384 || !Double.isFinite(temperature) || temperature < 0 || temperature > 2)
            throw new IllegalArgumentException("Invalid token limit or temperature");
    }
    @Override public String toString() {
        return "LlmConfig[provider=" + provider + ", model=" + model + ", apiKey=<redacted>]";
    }
}
