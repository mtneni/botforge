package org.legendstack.basebot.rag;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.annotation.UnfoldingTools;
import org.drivine.manager.PersistenceManager;
import org.legendstack.basebot.user.BotForgeUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@UnfoldingTools(name = "neo4j", description = "Tools for interacting with the Neo4j graph database")
@Service
public class Neo4jDatabaseTool {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jDatabaseTool.class);
    private final PersistenceManager persistenceManager;
    private final BotForgeUserService userService;

    public Neo4jDatabaseTool(PersistenceManager persistenceManager, BotForgeUserService userService) {
        this.persistenceManager = persistenceManager;
        this.userService = userService;
    }

    @LlmTool(description = """
            Execute a Cypher query against the Neo4j knowledge graph.
            Use this tool to precisely answer complex questions about relationships in the data.
            IMPORTANT: You MUST always filter your queries using the provided $teamId parameter.
            The schema includes nodes like:
            - Proposition {id, text, level, status, teamId}
            - Use MATCH (p:Proposition {teamId: $teamId}) WHERE p.text CONTAINS '...' RETURN p LIMIT 5
            """)
    public String executeCypher(String cypherQuery) {
        var teamId = userService.getAuthenticatedUser().getTeamId();
        logger.info("Executing LLM-generated Cypher query for team {}: {}", teamId, cypherQuery);
        try {
            var params = new HashMap<String, Object>();
            params.put("teamId", teamId);

            var rows = persistenceManager.query(
                    org.drivine.query.QuerySpecification
                            .withStatement(cypherQuery)
                            .bind(params)
                            .transform(Map.class));
            return rows.toString();
        } catch (Exception e) {
            return "Error executing cypher: " + e.getMessage();
        }
    }
}
