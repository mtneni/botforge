package org.legendstack.bot.architect;

import com.embabel.dice.common.Relations;
import org.legendstack.basebot.tools.DesignDocumentationTool;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for the Architect bot.
 * This bot focuses on high-level system design and technical governance.
 */
@Configuration
@Profile("architect")
public class ArchitectConfiguration {

    @Bean
    public DesignDocumentationTool architectureTools(BotForgeUserService userService) {
        return new DesignDocumentationTool(userService);
    }

    @Bean
    public Relations architecturalRelations() {
        return Relations.empty()
                .withSemanticBetween("SystemComponent", "SystemComponent", "calls",
                        "component calls another component")
                .withSemanticBetween("SystemComponent", "SystemComponent", "depends_on",
                        "component depends on another component")
                .withSemanticBetween("SystemComponent", "ApiEndpoint", "exposes",
                        "component exposes an API endpoint")
                .withSemanticBetween("SystemComponent", "ApiEndpoint", "consumes",
                        "component consumes an API endpoint")
                .withSemanticBetween("SystemComponent", "DataStore", "reads_from",
                        "component reads from a data store")
                .withSemanticBetween("SystemComponent", "DataStore", "writes_to",
                        "component writes to a data store")
                .withSemanticBetween("AIAgent", "MCPTool", "uses_tool",
                        "agent uses an MCP tool")
                .withSemanticBetween("AIAgent", "LLMModel", "calls_model",
                        "agent calls a specific LLM model")
                .withSemanticBetween("AIAgent", "SystemComponent", "integrates_with",
                        "agent integrates with a system component")
                .withSemanticBetween("AIAgent", "AIAgent", "delegates_to",
                        "agent delegates a sub-task to another agent")
                .withSemanticBetween("AIAgent", "DataStore", "indexed_in",
                        "agent's knowledge is indexed in a vector store");
    }
}
