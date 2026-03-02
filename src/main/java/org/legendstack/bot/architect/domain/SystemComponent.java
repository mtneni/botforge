package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents a software system component, such as a microservice,
 * module, or server-side application.
 */
public interface SystemComponent extends NamedEntity {

    @JsonPropertyDescription("Short description of what this component does")
    String getDescription();

    @JsonPropertyDescription("Type of component, e.g. 'microservice', 'monolith', 'lambda'")
    String getComponentType();
}
