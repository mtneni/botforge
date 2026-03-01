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

import java.util.*;

@RestController
@RequestMapping("/api/graph")
public class GraphRestController {

    private final Logger logger = LoggerFactory.getLogger(GraphRestController.class);
    private final PersistenceManager persistenceManager;
    private final BotForgeUserService userService;

    public GraphRestController(PersistenceManager persistenceManager, BotForgeUserService userService) {
        this.persistenceManager = persistenceManager;
        this.userService = userService;
    }

    @GetMapping("/data")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> getGraphData(@RequestParam(required = false) String contextId) {
        var user = userService.getAuthenticatedUser();
        var effectiveContextId = contextId != null ? contextId : user.effectiveContext();

        // Ensure effectiveContextId is never null to avoid Map errors
        if (effectiveContextId == null) {
            effectiveContextId = "personal"; // Safe fallback
        }

        var cypher = """
                MATCH (n)
                WHERE n.context = $contextId OR n.contextId = $contextId OR n:NamedEntity OR n:Proposition
                WITH n LIMIT 400
                OPTIONAL MATCH (n)-[r]->(m)
                WHERE m.context = $contextId OR m.contextId = $contextId OR m:NamedEntity OR m:Proposition
                RETURN
                    elementId(n) as sourceId, labels(n) as sourceLabels, properties(n) as sourceProps,
                    type(r) as relType,
                    elementId(m) as targetId, labels(m) as targetLabels, properties(m) as targetProps
                """;

        // Use mutable map instead of Map.of() since Drivine might modify or require
        // mutable params
        Map<String, Object> params = new HashMap<>();
        params.put("contextId", effectiveContextId);

        try {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) (List) persistenceManager.query(
                    QuerySpecification.withStatement(cypher)
                            .bind(params)
                            .transform(Map.class));

            if (rows == null) {
                return ResponseEntity.ok(Map.of("nodes", Collections.emptyList(), "links", Collections.emptyList()));
            }

            Map<String, Map<String, Object>> nodes = new HashMap<>();
            List<Map<String, Object>> links = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                if (row == null)
                    continue;

                Object sourceIdObj = row.get("sourceId");
                if (sourceIdObj == null)
                    continue;

                String sourceId = sourceIdObj.toString();
                if (!nodes.containsKey(sourceId)) {
                    nodes.put(sourceId, formatNode(sourceId, (List<String>) row.get("sourceLabels"),
                            (Map<String, Object>) row.get("sourceProps")));
                }

                Object targetIdObj = row.get("targetId");
                if (targetIdObj != null) {
                    String targetId = targetIdObj.toString();
                    if (!nodes.containsKey(targetId)) {
                        nodes.put(targetId, formatNode(targetId, (List<String>) row.get("targetLabels"),
                                (Map<String, Object>) row.get("targetProps")));
                    }

                    String relType = (String) row.get("relType");
                    Map<String, Object> link = new HashMap<>();
                    link.put("source", sourceId);
                    link.put("target", targetId);
                    link.put("type", relType);
                    links.add(link);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "nodes", nodes.values(),
                    "links", links));
        } catch (Exception e) {
            logger.error("Failed to fetch graph data", e);
            if (e.getMessage() != null && e.getMessage().contains("elementId")) {
                return retryWithNumericIds(effectiveContextId);
            }
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error", "stack",
                            sw.toString()));
        }
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<?> retryWithNumericIds(String effectiveContextId) {
        var cypher = """
                MATCH (n)
                WHERE n.context = $contextId OR n.contextId = $contextId OR n:NamedEntity OR n:Proposition
                WITH n LIMIT 400
                OPTIONAL MATCH (n)-[r]->(m)
                WHERE m.context = $contextId OR m.contextId = $contextId OR m:NamedEntity OR m:Proposition
                RETURN
                    id(n) as sourceId, labels(n) as sourceLabels, properties(n) as sourceProps,
                    type(r) as relType,
                    id(m) as targetId, labels(m) as targetLabels, properties(m) as targetProps
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("contextId", effectiveContextId);

        try {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) (List) persistenceManager.query(
                    QuerySpecification.withStatement(cypher)
                            .bind(params)
                            .transform(Map.class));
            Map<String, Map<String, Object>> nodes = new HashMap<>();
            List<Map<String, Object>> links = new ArrayList<>();

            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    if (row == null || row.get("sourceId") == null)
                        continue;

                    String sourceId = row.get("sourceId").toString();
                    if (!nodes.containsKey(sourceId)) {
                        nodes.put(sourceId, formatNode(sourceId, (List<String>) row.get("sourceLabels"),
                                (Map<String, Object>) row.get("sourceProps")));
                    }
                    if (row.get("targetId") != null) {
                        String targetId = row.get("targetId").toString();
                        if (!nodes.containsKey(targetId)) {
                            nodes.put(targetId, formatNode(targetId, (List<String>) row.get("targetLabels"),
                                    (Map<String, Object>) row.get("targetProps")));
                        }
                        Map<String, Object> link = new HashMap<>();
                        link.put("source", sourceId);
                        link.put("target", targetId);
                        link.put("type", (String) row.get("relType"));
                        links.add(link);
                    }
                }
            }
            return ResponseEntity.ok(Map.of("nodes", nodes.values(), "links", links));
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error", "stack",
                            sw.toString()));
        }
    }

    private Map<String, Object> formatNode(String id, List<String> labels, Map<String, Object> props) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", id);
        node.put("labels", labels != null ? labels : Collections.emptyList());

        String name = "Node";
        if (props != null) {
            if (props.containsKey("name"))
                name = (String) props.get("name");
            else if (props.containsKey("title"))
                name = (String) props.get("title");
            else if (props.containsKey("text")) {
                String text = (String) props.get("text");
                name = text != null && text.length() > 30 ? text.substring(0, 27) + "..." : text;
            }
        }
        if ("Node".equals(name) && labels != null && !labels.isEmpty()) {
            name = labels.get(0);
        }

        node.put("name", name);
        node.put("properties", props != null ? props : Collections.emptyMap());
        return node;
    }
}
