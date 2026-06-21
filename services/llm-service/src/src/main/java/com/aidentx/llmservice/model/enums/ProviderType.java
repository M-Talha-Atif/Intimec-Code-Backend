package com.aidentx.llmservice.model.enums;

/**
 * Enumeration of supported LLM providers.
 */
public enum ProviderType {
    GOOGLE_AI_STUDIO("Google AI Studio"),
    DEEPSEEK("Deepseek"),
    MISTRAL("Mistral");

    private final String displayName;

    ProviderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
