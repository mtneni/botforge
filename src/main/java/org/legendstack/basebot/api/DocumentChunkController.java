package org.legendstack.basebot.api;

import com.embabel.agent.rag.model.Chunk;
import com.embabel.agent.rag.store.ChunkingContentElementRepository;
import org.legendstack.basebot.rag.DocumentService;
import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.drivine.manager.PersistenceManager;
import org.drivine.query.QuerySpecification;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving document chunks for visualization.
 */
@RestController
@RequestMapping("/api/documents/chunks")
public class DocumentChunkController {

    private final PersistenceManager persistenceManager;
    private final BotForgeUserService userService;

    public DocumentChunkController(PersistenceManager persistenceManager,
            BotForgeUserService userService) {
        this.persistenceManager = persistenceManager;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getChunks(@RequestParam String uri) {
        // Validate user access
        userService.getAuthenticatedUser();

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DocumentChunkController.class);
        logger.debug("Retrieving chunks for URI: {}", uri);

        String cypher = """
                MATCH (c:Chunk)-[:HAS_PARENT*]->(root:ContentElement)
                WHERE root.uri = $uri OR root.uri = $normalizedUri
                RETURN c.index as index, c.text as text, properties(c) as props
                ORDER BY c.index ASC
                """;

        String normalizedTarget = DocumentService.normalizeUri(uri);

        try {
            List<Map<String, Object>> rows = (List<Map<String, Object>>) (List) persistenceManager.query(
                    QuerySpecification.<Map>withStatement(cypher)
                            .bind(Map.of("uri", uri, "normalizedUri", normalizedTarget))
                            .transform(Map.class));

            List<Map<String, Object>> chunks = rows.stream()
                    .map(row -> {
                        Map<String, Object> props = (Map<String, Object>) row.get("props");
                        // try to extract metadata if it exists
                        Object metadata = props != null ? props.get("metadata") : null;
                        
                        return Map.of(
                                "index", row.get("index") != null ? row.get("index") : 0,
                                "text", row.get("text") != null ? row.get("text") : "",
                                "metadata", metadata != null ? metadata : Map.of()
                        );
                    })
                    .collect(Collectors.toList());

            logger.info("Found {} chunks for URI: {} (Normalized: {})", chunks.size(), uri, normalizedTarget);
            return ResponseEntity.ok(Map.of("uri", uri, "chunks", chunks));
            
        } catch (Exception e) {
            logger.error("Error retrieving chunks for URI: " + uri, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
