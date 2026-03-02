package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents a network-accessible API endpoint or interface.
 */
public interface ApiEndpoint extends NamedEntity {

    @JsonPropertyDescription("The network path or URL pattern of the endpoint")
    String getPath();

    @JsonPropertyDescription("The protocol used, e.g. 'REST', 'gRPC', 'GraphQL'")
    String getProtocol();

    @JsonPropertyDescription("HTTP method if applicable, e.g. 'GET', 'POST'")
    String getMethod();
}
