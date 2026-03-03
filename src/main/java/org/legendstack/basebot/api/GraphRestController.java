package org.legendstack.basebot.api;

import org.legendstack.basebot.user.BotForgeUserService;
import org.drivine.manager.PersistenceManager;
import org.drivine.query.QuerySpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphRestController {

    private final Logger logger = LoggerFactory.getLogger(GraphRestController.class);
    private final PersistenceManager persistenceManager;
    private final BotForgeUserService userService;

    /**
     * Cypher template for fetching nodes only.
     * Neo4j 5.x uses {@code elementId()}, older versions use {@code id()}.
     * Scoped by teamId to enforce multi-tenant data isolation.
     * Returns only scalar values to avoid Drivine deserialization issues.
     */
    private static final String NODE_QUERY_TEMPLATE = """
            MATCH (n)
            WHERE (n.context = $contextId OR n.contextId = $contextId OR n:NamedEntity OR n:Proposition)
              AND coalesce(n.teamId, $teamId) = $teamId
            WITH n LIMIT 400
            RETURN %s(n) as nodeId,
                   head(labels(n)) as nodeLabel,
                   coalesce(n.name, n.title, n.text, head(labels(n))) as nodeName
            """;

    /**
     * Cypher template for fetching relationships between known nodes.
     * Returns only scalar values.
     */
    private static final String LINK_QUERY_TEMPLATE = """
            MATCH (n)-[r]->(m)
            WHERE (n.context = $contextId OR n.contextId = $contextId OR n:NamedEntity OR n:Proposition)
              AND coalesce(n.teamId, $teamId) = $teamId
              AND (m.context = $contextId OR m.contextId = $contextId OR m:NamedEntity OR m:Proposition)
              AND coalesce(m.teamId, $teamId) = $teamId
            RETURN %s(n) as sourceId, type(r) as relType, %s(m) as targetId,
                   head(labels(m)) as targetLabel,
                   coalesce(m.name, m.title, m.text, head(labels(m))) as targetName
            LIMIT 800
            """;

    public GraphRestController(PersistenceManager persistenceManager, BotForgeUserService userService) {
        this.persistenceManager = persistenceManager;
        this.userService = userService;
    }

    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getGraphData(@RequestParam(required = false) String contextId) {
        var user = userService.getAuthenticatedUser();
        var effectiveContextId = contextId != null ? contextId : user.effectiveContext();
        if (effectiveContextId == null) {
            effectiveContextId = "personal";
        }

        try {
            return ResponseEntity.ok(executeGraphQuery(effectiveContextId, user.getTeamId(), "elementId"));
        } catch (Exception e) {
            logger.warn("Graph query with elementId failed, trying id() fallback: {}", e.getMessage());
            try {
                return ResponseEntity.ok(executeGraphQuery(effectiveContextId, user.getTeamId(), "id"));
            } catch (Exception fallbackError) {
                logger.error("Graph query fallback also failed", fallbackError);
                return errorResponse(fallbackError);
            }
        }
    }

    /**
     * Executes two separate graph queries (nodes + links) to avoid OPTIONAL MATCH
     * null issues in Drivine's result mapper, and transforms the result into a
     * nodes + links structure.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeGraphQuery(String contextId, String teamId, String idFunction) {
        Map<String, Object> params = new HashMap<>();
        params.put("contextId", contextId);
        params.put("teamId", teamId);

        // --- Query 1: Fetch all nodes ---
        String nodeCypher = NODE_QUERY_TEMPLATE.formatted(idFunction);
        List<Map<String, Object>> nodeRows;
        try {
            nodeRows = (List<Map<String, Object>>) (List) persistenceManager.query(
                    QuerySpecification.withStatement(nodeCypher)
                            .bind(params)
                            .transform(Map.class));
        } catch (Exception e) {
            logger.warn("Node query failed: {}", e.getMessage());
            nodeRows = Collections.emptyList();
        }

        Map<String, Map<String, Object>> nodes = new HashMap<>();
        if (nodeRows != null) {
            for (Map<String, Object> row : nodeRows) {
                if (row == null || row.get("nodeId") == null)
                    continue;
                String nodeId = row.get("nodeId").toString();
                String label = row.get("nodeLabel") != null ? row.get("nodeLabel").toString() : "Node";
                String name = row.get("nodeName") != null ? row.get("nodeName").toString() : label;
                nodes.computeIfAbsent(nodeId, id -> {
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", id);
                    node.put("labels", List.of(label));
                    node.put("name", truncate(name, 40));
                    node.put("properties", Collections.emptyMap());
                    return node;
                });
            }
        }

        // --- Query 2: Fetch relationships ---
        String linkCypher = LINK_QUERY_TEMPLATE.formatted(idFunction, idFunction);
        List<Map<String, Object>> linkRows;
        try {
            linkRows = (List<Map<String, Object>>) (List) persistenceManager.query(
                    QuerySpecification.withStatement(linkCypher)
                            .bind(params)
                            .transform(Map.class));
        } catch (Exception e) {
            logger.warn("Link query failed: {}", e.getMessage());
            linkRows = Collections.emptyList();
        }

        List<Map<String, Object>> links = new ArrayList<>();
        if (linkRows != null) {
            for (Map<String, Object> row : linkRows) {
                if (row == null || row.get("sourceId") == null || row.get("targetId") == null)
                    continue;

                String sourceId = row.get("sourceId").toString();
                String targetId = row.get("targetId").toString();

                // Ensure target node is in the node map
                nodes.computeIfAbsent(targetId, id -> {
                    String tLabel = row.get("targetLabel") != null ? row.get("targetLabel").toString() : "Node";
                    String tName = row.get("targetName") != null ? row.get("targetName").toString() : tLabel;
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", id);
                    node.put("labels", List.of(tLabel));
                    node.put("name", truncate(tName, 40));
                    node.put("properties", Collections.emptyMap());
                    return node;
                });

                Map<String, Object> link = new HashMap<>();
                link.put("source", sourceId);
                link.put("target", targetId);
                link.put("type", row.get("relType"));
                links.add(link);
            }
        }

        return Map.of("nodes", nodes.values(), "links", links);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null)
            return "Node";
        return s.length() > maxLen ? s.substring(0, maxLen - 3) + "..." : s;
    }

    private ResponseEntity<Map<String, Object>> errorResponse(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "error", e.getMessage() != null ? e.getMessage() : "Unknown error",
                        "stack", sw.toString()));
    }
}
