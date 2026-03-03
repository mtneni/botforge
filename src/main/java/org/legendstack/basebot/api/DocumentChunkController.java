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
import java.util.stream.StreamSupport;

/**
 * REST controller for retrieving document chunks for visualization.
 */
@RestController
@RequestMapping("/api/documents/chunks")
public class DocumentChunkController {

    private final ChunkingContentElementRepository contentRepository;
    private final BotForgeUserService userService;

    public DocumentChunkController(ChunkingContentElementRepository contentRepository,
            BotForgeUserService userService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getChunks(@RequestParam String uri) {
        // Validate user access
        userService.getAuthenticatedUser();

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DocumentChunkController.class);
        logger.debug("Retrieving chunks for URI: {}", uri);

        String normalizedTarget = DocumentService.normalizeUri(uri);

        var chunks = StreamSupport.stream(contentRepository.findAll(Chunk.class).spliterator(), false)
                .filter(chunk -> {
                    try {
                        Object parentUriObj = null;
                        try {
                            var method = chunk.getClass().getMethod("parentUri");
                            parentUriObj = method.invoke(chunk);
                        } catch (Exception e) {
                            var method = chunk.getClass().getMethod("getParentUri");
                            parentUriObj = method.invoke(chunk);
                        }

                        if (parentUriObj == null)
                            return false;
                        String parentUri = parentUriObj.toString();

                        boolean match = normalizedTarget.equals(DocumentService.normalizeUri(parentUri));
                        if (!match && logger.isTraceEnabled()) {
                            logger.trace("URI mismatch: target={} ({}) vs chunk={} ({})",
                                    uri, normalizedTarget, parentUri, DocumentService.normalizeUri(parentUri));
                        }
                        return match;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(chunk -> {
                    try {
                        return Map.of(
                                "index", invoke(chunk, "index", "getIndex"),
                                "text", invoke(chunk, "text", "getText", "getContent"),
                                "metadata", invoke(chunk, "metadata", "getMetadata"));
                    } catch (Exception e) {
                        return Map.of("error", e.getMessage());
                    }
                })
                .toList();

        logger.info("Found {} chunks for URI: {} (Normalized: {})", chunks.size(), uri, normalizedTarget);
        return ResponseEntity.ok(Map.of("uri", uri, "chunks", chunks));
    }

    private String normalizeUri(String uri) {
        return DocumentService.normalizeUri(uri);
    }

    private Object invoke(Object obj, String... methodNames) {
        for (String name : methodNames) {
            try {
                return obj.getClass().getMethod(name).invoke(obj);
            } catch (Exception e) {
                // try next
            }
        }
        return null;
    }
}
