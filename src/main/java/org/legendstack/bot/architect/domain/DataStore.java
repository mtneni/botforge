package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents a data persistence or storage system.
 */
public interface DataStore extends NamedEntity {

    @JsonPropertyDescription("Type of data store, e.g. 'PostgreSQL', 'Redis', 'S3'")
    String getStoreType();

    @JsonPropertyDescription("Primary purpose of this store, e.g. 'Relational Data', 'Cache', 'Blob Storage'")
    String getPurpose();
}
