package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents an Agentic AI entity capable of reasoning and tool use.
 */
public interface AIAgent extends NamedEntity {

    @JsonPropertyDescription("Primary role and goal of the agent")
    String getRole();

    @JsonPropertyDescription("Short description of agency capabilities")
    String getDescription();

    @JsonPropertyDescription("Patterns used, e.g. 'Reflexion', 'Planning', 'Tool Use'")
    String getAgenticPatterns();
}
