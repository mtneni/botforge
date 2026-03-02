package org.legendstack.basebot.rag;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.annotation.UnfoldingTools;
import com.embabel.dice.proposition.Proposition;
import org.legendstack.basebot.user.BotForgeUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LLM-callable tool that exposes hybrid search (vector + keyword) to the agent.
 * Injected into the ResponsePipeline alongside DICE Memory so the LLM can
 * choose the best retrieval strategy for any query.
 */
@UnfoldingTools(name = "hybridSearchTool", description = "Advanced hybrid search combining semantic similarity and keyword matching")
@Service
public class HybridSearchTool {

    private static final Logger logger = LoggerFactory.getLogger(HybridSearchTool.class);

    private final HybridSearchService hybridSearchService;
    private final BotForgeUserService userService;

    public HybridSearchTool(HybridSearchService hybridSearchService,
            BotForgeUserService userService) {
        this.hybridSearchService = hybridSearchService;
        this.userService = userService;
    }

    @LlmTool(description = """
            Search the knowledge base using hybrid retrieval (vector similarity + keyword matching).
            This is more powerful than memory search alone — it combines semantic understanding
            with exact text matching using Reciprocal Rank Fusion (RRF).

            Use this when:
            - The user asks about specific technical terms, names, or identifiers
            - You need both conceptual and exact-match results
            - Memory search returns insufficient or irrelevant results

            The query should be a natural language description of what you're looking for.
            Returns the top matching propositions from the knowledge graph.
            """)
    public String hybridSearch(String query) {
        try {
            var user = userService.getAuthenticatedUser();
            List<Proposition> results = hybridSearchService.hybridSearch(query, user.getTeamId(), 10);

            if (results.isEmpty()) {
                return "No results found for: " + query;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Found %d results for: %s\n\n", results.size(), query));
            for (int i = 0; i < results.size(); i++) {
                sb.append(String.format("%d. %s\n", i + 1, results.get(i)));
            }
            return sb.toString();

        } catch (Exception e) {
            logger.error("Hybrid search tool failed for query: {}", query, e);
            return "Search failed: " + e.getMessage();
        }
    }
}
