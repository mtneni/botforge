package org.legendstack.bot.architect.domain;

import com.embabel.agent.rag.model.NamedEntity;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Represents an MCP (Model Context Protocol) tool or server.
 */
public interface MCPTool extends NamedEntity {

    @JsonPropertyDescription("Formal name of the tool")
    String getToolName();

    @JsonPropertyDescription("Description of what the tool accomplishes")
    String getDescription();

    @JsonPropertyDescription("The MCP server hosting this tool, e.g. 'brave-search', 'filesystem'")
    String getMcpServer();
}
