package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents an engineering team or organizational unit responsible for
 * owning and maintaining system components.
 */
public interface Team extends NamedEntity {

    @JsonPropertyDescription("Team function: 'frontend', 'backend', 'platform', 'SRE', 'data-engineering', 'ML', 'security', 'QA', 'DevOps'")
    String getRole();

    @JsonPropertyDescription("Team size or headcount, e.g. '5', '12'")
    String getHeadcount();

    @JsonPropertyDescription("Team's primary tech stack or domain focus, e.g. 'React/TypeScript', 'Spring Boot/Java', 'Python/ML'")
    String getTechFocus();
}
