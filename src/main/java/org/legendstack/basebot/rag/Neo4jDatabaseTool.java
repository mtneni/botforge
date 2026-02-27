package org.legendstack.basebot.rag;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.annotation.UnfoldingTools;
import org.drivine.manager.PersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@UnfoldingTools(name = "neo4j", description = "Tools for interacting with the Neo4j graph database")
@Service
public class Neo4jDatabaseTool {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jDatabaseTool.class);
    private final PersistenceManager persistenceManager;

    public Neo4jDatabaseTool(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @LlmTool(description = """
            Execute a Cypher query against the Neo4j knowledge graph.
            Use this tool to precisely answer complex questions about relationships in the data.
            The schema includes nodes like:
            - ProcessedChunk: actual document chunks (properties: text, title, documentId)
            - Object nodes, Proposition nodes, Mention nodes connected by 'IS_ABOUT', 'OWNS', etc.
            - Use MATCH (c:ProcessedChunk) WHERE c.text CONTAINS '...' RETURN c LIMIT 5 for raw text search.
            """)
    public String executeCypher(String cypherQuery) {
        logger.info("Executing LLM-generated Cypher query: {}", cypherQuery);
        try {
            var rows = persistenceManager.query(
                    org.drivine.query.QuerySpecification
                            .withStatement(cypherQuery)
                            .transform(Map.class));
            return rows.toString();
        } catch (Exception e) {
            return "Error executing cypher: " + e.getMessage();
        }
    }
}
