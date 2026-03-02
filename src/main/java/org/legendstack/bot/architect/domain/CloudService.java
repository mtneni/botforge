package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents a cloud or infrastructure service used by the architecture.
 */
public interface CloudService extends NamedEntity {

    @JsonPropertyDescription("Cloud provider: 'AWS', 'Azure', 'GCP', 'On-Prem', 'Multi-Cloud'")
    String getProvider();

    @JsonPropertyDescription("Service category: 'compute', 'storage', 'database', 'messaging', 'networking', 'AI-ML', 'monitoring', 'CDN', 'identity'")
    String getServiceCategory();

    @JsonPropertyDescription("Hosting tier: 'managed', 'serverless', 'self-hosted', 'SaaS'")
    String getTier();

    @JsonPropertyDescription("Specific service name, e.g. 'EKS', 'Lambda', 'Cloud Run', 'RDS Aurora'")
    String getServiceName();
}
