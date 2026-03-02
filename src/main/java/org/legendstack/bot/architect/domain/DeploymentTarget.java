package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents a deployment target environment where system components
 * are deployed and operated.
 */
public interface DeploymentTarget extends NamedEntity {

    @JsonPropertyDescription("Environment stage: 'development', 'staging', 'production', 'DR'")
    String getEnvironment();

    @JsonPropertyDescription("Compute platform: 'Kubernetes', 'ECS', 'Lambda', 'Cloud Run', 'VM', 'Bare Metal', 'Edge'")
    String getPlatform();

    @JsonPropertyDescription("Geographic region or availability zone, e.g. 'us-east-1', 'eu-west-2', 'global'")
    String getRegion();

    @JsonPropertyDescription("Deployment strategy: 'blue-green', 'canary', 'rolling', 'recreate', 'feature-flag'")
    String getStrategy();
}
