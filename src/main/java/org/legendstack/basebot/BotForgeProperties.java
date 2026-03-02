package org.legendstack.basebot;

import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.neo.drivine.NeoRagServiceProperties;
import org.legendstack.basebot.proposition.PropositionExtractionProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Properties for chatbot
 *
 * @param chat             chatbot conversational configuration (LLM, persona,
 *                         objective, etc.)
 * @param ingestion        configuration for ingestion
 * @param neoRag           Neo4j RAG service configuration
 * @param memory           proposition extraction configuration
 * @param botPackages      additional packages to scan for bot components (e.g.,
 *                         "org.legendstack.bot").
 *                         Beans in these packages should be gated with
 *                         {@code @Profile}.
 * @param initialDocuments list of document URIs to ingest into the global
 *                         context at startup
 *                         if not already loaded. Each entry can be a URL (e.g.,
 *                         "https://example.com/doc.pdf")
 *                         or a file path (absolute or relative to the working
 *                         directory).
 * @param stylesheet       optional additional stylesheet to load (e.g.,
 *                         "architect"). When set,
 *                         loads {@code themes/BotForge/<stylesheet>.css} as an
 *                         override on top
 *                         of the base theme.
 */
@ConfigurationProperties(prefix = "botforge")
public record BotForgeProperties(
        @NestedConfigurationProperty ChatbotOptions chat,
        @NestedConfigurationProperty ContentChunker.Config ingestion,
        @NestedConfigurationProperty NeoRagServiceProperties neoRag,
        @NestedConfigurationProperty PropositionExtractionProperties memory,
        @DefaultValue("") List<String> botPackages,
        List<String> initialDocuments,
        @DefaultValue("") String stylesheet,
        @DefaultValue("use for web search") String mcpToolsDescription) {

    public BotForgeProperties {
        if (neoRag == null) {
            neoRag = new NeoRagServiceProperties();
        }
    }

    /**
     * Returns a copy of these properties with the chat options replaced.
     * Avoids fragile manual reconstruction of all record fields.
     */
    public BotForgeProperties withChat(ChatbotOptions newChat) {
        return new BotForgeProperties(newChat, ingestion, neoRag, memory,
                botPackages, initialDocuments, stylesheet, mcpToolsDescription);
    }

}
