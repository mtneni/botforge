package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents an enterprise integration pattern used to connect
 * system components — a key concern in distributed architecture.
 */
public interface IntegrationPattern extends NamedEntity {

    @JsonPropertyDescription("Pattern type: 'event-driven', 'request-reply', 'saga', 'CQRS', 'pub-sub', 'API-gateway', 'service-mesh', 'circuit-breaker', 'bulkhead', 'retry', 'dead-letter-queue'")
    String getPatternType();

    @JsonPropertyDescription("Communication protocol: 'HTTP', 'gRPC', 'AMQP', 'Kafka', 'WebSocket', 'GraphQL', 'NATS'")
    String getProtocol();

    @JsonPropertyDescription("Whether this is synchronous or asynchronous: 'sync', 'async'")
    String getCommunicationStyle();
}
