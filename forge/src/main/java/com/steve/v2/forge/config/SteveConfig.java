package com.steve.v2.forge.config;

import com.steve.v2.core.llm.LlmConfig;
import java.net.URI;
import java.util.Locale;
import net.minecraftforge.common.ForgeConfigSpec;

public final class SteveConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_KEY, OPENAI_MODEL, GROQ_KEY, GROQ_MODEL, GEMINI_KEY, GEMINI_MODEL;
    public static final ForgeConfigSpec.IntValue MAX_STEVES, MAX_TOKENS;
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE;
    public static final ForgeConfigSpec.BooleanValue ASSISTANT_MODE, IDLE_FOLLOW;
    static {
        var b = new ForgeConfigSpec.Builder();
        b.push("ai");
        PROVIDER = b.define("provider", "groq");
        MAX_TOKENS = b.defineInRange("maxTokens", 2048, 128, 16384);
        TEMPERATURE = b.defineInRange("temperature", 0.3, 0, 2);
        b.pop().push("openai");
        OPENAI_KEY = b.define("apiKey", ""); OPENAI_MODEL = b.define("model", "");
        b.pop().push("groq");
        GROQ_KEY = b.define("apiKey", ""); GROQ_MODEL = b.define("model", "");
        b.pop().push("gemini");
        GEMINI_KEY = b.define("apiKey", ""); GEMINI_MODEL = b.define("model", "");
        b.pop().push("behavior");
        MAX_STEVES = b.defineInRange("maxStevesPerOwner", 10, 1, 50);
        ASSISTANT_MODE = b.comment("Creative assistant behavior: flying, teleporting, invulnerable. Full survival is not implemented.").define("assistantMode", true);
        IDLE_FOLLOW = b.define("followOwnerWhenIdle", true);
        b.pop();
        SPEC = b.build();
    }
    public static LlmConfig llm() {
        String provider = PROVIDER.get().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "openai" -> new LlmConfig(LlmConfig.Provider.OPENAI, URI.create("https://api.openai.com/v1/chat/completions"),
                OPENAI_KEY.get(), OPENAI_MODEL.get(), MAX_TOKENS.get(), TEMPERATURE.get());
            case "groq" -> new LlmConfig(LlmConfig.Provider.GROQ, URI.create("https://api.groq.com/openai/v1/chat/completions"),
                GROQ_KEY.get(), GROQ_MODEL.get(), MAX_TOKENS.get(), TEMPERATURE.get());
            case "gemini" -> {
                String model = GEMINI_MODEL.get();
                if (!model.matches("[a-zA-Z0-9._-]+")) throw new IllegalArgumentException("Configure gemini.model");
                yield new LlmConfig(LlmConfig.Provider.GEMINI, URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"),
                    GEMINI_KEY.get(), model, MAX_TOKENS.get(), TEMPERATURE.get());
            }
            default -> throw new IllegalArgumentException("Unknown AI provider: " + provider);
        };
    }
    private SteveConfig() {}
}
