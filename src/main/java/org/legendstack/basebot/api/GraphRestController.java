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
     * Cypher template with a placeholder for the ID function name.
     * Neo4j 5.x uses {@code elementId()}, older versions use {@code id()}.
     * Scoped by teamId to enforce multi-tenant data isolation.
     */
    private static final String GRAPH_QUERY_TEMPLATE = """
            MATCH (n)
            WHERE (n.context = $contextId OR n.contextId = $contextId OR n:NamedEntity OR n:Proposition)
              AND (n.teamId = $teamId OR NOT EXISTS(n.teamId))
            WITH n LIMIT 400
            OPTIONAL MATCH (n)-[r]->(m)
            WHERE (m.context = $contextId OR m.contextId = $contextId OR m:NamedEntity OR m:Proposition)
              AND (m.teamId = $teamId OR NOT EXISTS(m.teamId))
            RETURN
                %s(n) as sourceId, labels(n) as sourceLabels, properties(n) as sourceProps,
                type(r) as relType,
                %s(m) as targetId, labels(m) as targetLabels, properties(m) as targetProps
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
            logger.error("Failed to fetch graph data", e);
            if (e.getMessage() != null && e.getMessage().contains("elementId")) {
                try {
                    return ResponseEntity.ok(executeGraphQuery(effectiveContextId, user.getTeamId(), "id"));
                } catch (Exception fallbackError) {
                    return errorResponse(fallbackError);
                }
            }
            return errorResponse(e);
        }
    }

    /**
     * Executes the graph query with the specified ID function (elementId or id)
     * and transforms the result into a nodes + links structure.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeGraphQuery(String contextId, String teamId, String idFunction) {
        String cypher = GRAPH_QUERY_TEMPLATE.formatted(idFunction, idFunction);

        Map<String, Object> params = new HashMap<>();
        params.put("contextId", contextId);
        params.put("teamId", teamId);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) (List) persistenceManager.query(
                QuerySpecification.withStatement(cypher)
                        .bind(params)
                        .transform(Map.class));

        if (rows == null) {
            return Map.of("nodes", Collections.emptyList(), "links", Collections.emptyList());
        }

        Map<String, Map<String, Object>> nodes = new HashMap<>();
        List<Map<String, Object>> links = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            if (row == null || row.get("sourceId") == null) {
                continue;
            }

            String sourceId = row.get("sourceId").toString();
            nodes.computeIfAbsent(sourceId, id -> formatNode(id, (List<String>) row.get("sourceLabels"),
                    (Map<String, Object>) row.get("sourceProps")));

            Object targetIdObj = row.get("targetId");
            if (targetIdObj != null) {
                String targetId = targetIdObj.toString();
                nodes.computeIfAbsent(targetId, id -> formatNode(id, (List<String>) row.get("targetLabels"),
                        (Map<String, Object>) row.get("targetProps")));

                Map<String, Object> link = new HashMap<>();
                link.put("source", sourceId);
                link.put("target", targetId);
                link.put("type", row.get("relType"));
                links.add(link);
            }
        }

        return Map.of("nodes", nodes.values(), "links", links);
    }

    private Map<String, Object> formatNode(String id, List<String> labels, Map<String, Object> props) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", id);
        node.put("labels", labels != null ? labels : Collections.emptyList());

        String name = resolveName(labels, props);
        node.put("name", name);
        node.put("properties", props != null ? props : Collections.emptyMap());
        return node;
    }

    private String resolveName(List<String> labels, Map<String, Object> props) {
        if (props != null) {
            if (props.containsKey("name"))
                return (String) props.get("name");
            if (props.containsKey("title"))
                return (String) props.get("title");
            if (props.containsKey("text")) {
                String text = (String) props.get("text");
                return text != null && text.length() > 30 ? text.substring(0, 27) + "..." : text;
            }
        }
        if (labels != null && !labels.isEmpty()) {
            return labels.get(0);
        }
        return "Node";
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
