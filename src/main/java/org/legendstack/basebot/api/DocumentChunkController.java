package org.legendstack.basebot.api;

import com.embabel.agent.rag.model.Chunk;
import com.embabel.agent.rag.store.ChunkingContentElementRepository;
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

    public DocumentChunkController(ChunkingContentElementRepository contentRepository, BotForgeUserService userService) {
        this.contentRepository = contentRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getChunks(@RequestParam String uri) {
        // Validate user access
        userService.getAuthenticatedUser();

        var chunks = StreamSupport.stream(contentRepository.findAll(Chunk.class).spliterator(), false)
                .filter(chunk -> {
                    try {
                        var method = chunk.getClass().getMethod("parentUri");
                        return uri.equals(method.invoke(chunk));
                    } catch (Exception e) {
                        try {
                            var method = chunk.getClass().getMethod("getParentUri");
                            return uri.equals(method.invoke(chunk));
                        } catch (Exception e2) {
                            return false;
                        }
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

        return ResponseEntity.ok(Map.of("uri", uri, "chunks", chunks));
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
