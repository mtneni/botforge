package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents a security requirement, compliance standard, or threat
 * mitigation control applicable to system components and APIs.
 */
public interface SecurityRequirement extends NamedEntity {

    @JsonPropertyDescription("Category of security control, e.g. 'authentication', 'authorization', 'encryption', 'network-security', 'data-protection', 'audit-logging'")
    String getCategory();

    @JsonPropertyDescription("Severity level: 'critical', 'high', 'medium', 'low'")
    String getSeverity();

    @JsonPropertyDescription("Compliance standard or framework, e.g. 'SOC2', 'GDPR', 'HIPAA', 'PCI-DSS', 'ISO-27001', 'OWASP', 'FedRAMP'")
    String getStandard();

    @JsonPropertyDescription("Current implementation status: 'implemented', 'partial', 'planned', 'missing'")
    String getStatus();
}
