package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents a non-functional quality attribute (ISO 25010) that
 * constrains or defines the acceptable behavior of a system.
 */
public interface QualityAttribute extends NamedEntity {

    @JsonPropertyDescription("Quality category: 'performance', 'scalability', 'availability', 'reliability', 'maintainability', 'security', 'observability', 'testability', 'deployability'")
    String getCategory();

    @JsonPropertyDescription("Quantitative target or SLA, e.g. 'p99 < 200ms', '99.95% uptime', 'RPO < 1h'")
    String getMeasurement();

    @JsonPropertyDescription("Priority: 'must-have', 'should-have', 'nice-to-have'")
    String getPriority();
}
