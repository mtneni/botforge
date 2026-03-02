package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents a Large Language Model used by agents.
 */
public interface LLMModel extends NamedEntity {

    @JsonPropertyDescription("Model ID, e.g. 'gpt-4o', 'claude-3-5-sonnet'")
    String getModelId();

    @JsonPropertyDescription("Model provider, e.g. 'OpenAI', 'Anthropic', 'Local'")
    String getProvider();

    @JsonPropertyDescription("Context window size, e.g. '128k', '200k'")
    String getContextWindow();
}
