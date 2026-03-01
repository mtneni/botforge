package org.legendstack.basebot.rag;

import com.embabel.agent.api.common.AiBuilder;
import org.legendstack.basebot.BotForgeProperties;
import org.drivine.manager.PersistenceManager;
import org.drivine.query.QuerySpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SubconsciousJanitorService {

    /**
     * Allowlist of relationship types that the janitor is permitted to create.
     * Any type returned by the LLM that is not in this set will be rejected,
     * eliminating the Cypher injection risk of dynamic MERGE statements.
     */
    private static final Set<String> ALLOWED_RELATIONSHIP_TYPES = Set.of(
            "RELATES_TO", "DEPENDS_ON", "IS_PART_OF", "CONTRADICTS",
            "SIMILAR_TO", "CAUSED_BY", "LEADS_TO", "BELONGS_TO",
            "ASSOCIATED_WITH", "DERIVED_FROM", "COMPLEMENTS",
            "LIKES", "DISLIKES", "KNOWS", "IS_INTERESTED_IN",
            "WORKS_ON", "PREFERS");
    private final Logger logger = LoggerFactory.getLogger(SubconsciousJanitorService.class);

    private final AiBuilder aiBuilder;
    private final BotForgeProperties properties;
    private final PersistenceManager persistenceManager;

    public SubconsciousJanitorService(AiBuilder aiBuilder, BotForgeProperties properties,
            PersistenceManager persistenceManager) {
        this.aiBuilder = aiBuilder;
        this.properties = properties;
        this.persistenceManager = persistenceManager;
    }

    public record SubconsciousInsight(String relationshipType) {
    }

    @Scheduled(fixedDelayString = "${botforge.subconscious.interval:300000}")
    @SuppressWarnings("unchecked")
    public void maintainGraph() {
        logger.info("Subconscious Janitor Service waking up to curate the Knowledge Graph...");

        // Find entities in the same context that don't have a direct edge
        String cypherFindPairs = """
                    MATCH (p1:NamedEntity), (p2:NamedEntity)
                    WHERE p1.context = p2.context
                      AND id(p1) > id(p2)
                      AND NOT (p1)-[]-(p2)
                    RETURN id(p1) as p1Id, p1.name as p1Name, p1.type as p1Type,
                           id(p2) as p2Id, p2.name as p2Name, p2.type as p2Type,
                           p1.context as context
                    LIMIT 5
                """;

        try {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) (List) persistenceManager.query(
                    QuerySpecification.withStatement(cypherFindPairs).transform(Map.class));

            if (rows == null || rows.isEmpty()) {
                logger.info("Janitor found no isolated pairs to link. Graph is fully curated.");
                return;
            }

            logger.info("Janitor discovered {} potential relationship pairs. Consulting LLM...", rows.size());

            var ai = aiBuilder
                    .withShowPrompts(properties.memory().showPrompts())
                    .withShowLlmResponses(properties.memory().showResponses())
                    .ai();

            for (Map<String, Object> row : rows) {
                Long p1Id = ((Number) row.get("p1Id")).longValue();
                String p1Name = (String) row.get("p1Name");
                String p1Type = (String) row.get("p1Type");

                Long p2Id = ((Number) row.get("p2Id")).longValue();
                String p2Name = (String) row.get("p2Name");
                String p2Type = (String) row.get("p2Type");

                String contextStr = (String) row.get("context");

                String prompt = """
                        You are the subconscious memory curator of an AI system.
                        Your job is to deduce if a meaningful relationship exists between two entities in the same context.

                        Context: %s

                        Entity 1: %s (Type: %s)
                        Entity 2: %s (Type: %s)

                        Do these two entities share a direct, meaningful relationship?
                        If so, return ONLY the relationship type as a single UPPERCASE snake_case string (e.g., RELATES_TO, DEPENDS_ON, IS_PART_OF, CONTRADICTS).
                        If they are unrelated or you are unsure, reply with ONLY the word NONE.
                        Do not provide any other explanation or text.
                        """
                        .formatted(contextStr, p1Name, p1Type, p2Name, p2Type);

                SubconsciousInsight insight = ai.withLlm(properties.memory().extractionLlm()).createObject(prompt,
                        SubconsciousInsight.class);
                String relType = insight.relationshipType();

                if (relType != null) {
                    relType = relType.trim().toUpperCase();
                    if (relType.equals("NONE")) {
                        continue;
                    }
                    if (!ALLOWED_RELATIONSHIP_TYPES.contains(relType)) {
                        logger.warn("Janitor rejected unknown relationship type '{}' between ({}) and ({})",
                                relType, p1Name, p2Name);
                        continue;
                    }

                    logger.info("\uD83E\uDDE0 Subconscious Insight: ({}) -[{}]-> ({})", p1Name, relType, p2Name);

                    // Safe: relType is guaranteed to be in the allowlist
                    String cypherMerge = """
                                MATCH (p1), (p2)
                                WHERE id(p1) = $p1Id AND id(p2) = $p2Id
                                MERGE (p1)-[r:%s]->(p2)
                                SET r.source = 'subconscious_janitor'
                            """.formatted(relType);

                    Map<String, Object> params = new HashMap<>();
                    params.put("p1Id", p1Id);
                    params.put("p2Id", p2Id);

                    persistenceManager.execute(QuerySpecification.withStatement(cypherMerge).bind(params));
                }
            }
        } catch (Exception e) {
            logger.error("Subconscious Janitor encountered an error parsing the graph", e);
        }
    }
}
