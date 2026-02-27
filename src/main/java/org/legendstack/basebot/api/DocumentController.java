package org.legendstack.basebot.api;

import org.legendstack.basebot.user.BotForgeUser;
import org.legendstack.basebot.rag.DocumentService;
import org.legendstack.basebot.user.BotForgeUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST endpoints for document management (upload, URL ingest, list, delete,
 * stats).
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final BotForgeUserService userService;

    public DocumentController(DocumentService documentService, BotForgeUserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> listDocuments(@RequestParam(required = false) String context) {
        var user = userService.getAuthenticatedUser();
        var effectiveContext = resolveContext(context, user);
        var docs = documentService.getDocuments(effectiveContext).stream()
                .map(doc -> Map.of(
                        "uri", doc.uri(),
                        "title", doc.title() != null ? doc.title() : doc.uri(),
                        "context", doc.context(),
                        "chunkCount", doc.chunkCount(),
                        "ingestedAt", doc.ingestedAt() != null ? doc.ingestedAt().toString() : ""))
                .toList();
        return ResponseEntity.ok(docs);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "personal") String context) {
        try {
            var user = userService.getAuthenticatedUser();
            var docContext = "global".equals(context)
                    ? DocumentService.Context.global(user)
                    : new DocumentService.Context(user);

            // Use content-based URI to avoid duplicates and handle same-name-diff-content
            byte[] bytes = file.getBytes();
            var contentHash = documentService.computeHash(bytes);
            var uri = "upload://" + docContext.effectiveContext() + "/" + contentHash + "/"
                    + file.getOriginalFilename();

            if (documentService.exists(uri)) {
                return ResponseEntity.ok(Map.of(
                        "message", "Already indexed: " + file.getOriginalFilename(),
                        "uri", uri));
            }

            documentService.ingestBytes(bytes, uri, file.getOriginalFilename(), docContext);
            return ResponseEntity.ok(Map.of(
                    "message", "Uploaded: " + file.getOriginalFilename(),
                    "uri", uri));
        } catch (Exception e) {
            logger.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    public record UrlRequest(String url) {
    }

    @PostMapping("/url")
    public ResponseEntity<?> ingestUrl(@RequestBody UrlRequest request,
            @RequestParam(defaultValue = "personal") String context) {
        var user = userService.getAuthenticatedUser();
        var docContext = "global".equals(context)
                ? DocumentService.Context.global(user)
                : new DocumentService.Context(user);

        var url = request.url();
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            documentService.ingestUrl(url, docContext);
            return ResponseEntity.ok(Map.of("message", "Ingested: " + url));
        } catch (Exception e) {
            logger.error("Failed to ingest URL: {}", url, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteDocument(@RequestParam String uri) {
        if (documentService.deleteDocument(uri)) {
            return ResponseEntity.ok(Map.of("message", "Deleted: " + uri));
        }
        return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete document"));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestParam(required = false) String context) {
        var user = userService.getAuthenticatedUser();
        var effectiveContext = resolveContext(context, user);
        return ResponseEntity.ok(Map.of(
                "documentCount", documentService.getDocumentCount(effectiveContext),
                "chunkCount", documentService.getChunkCount(effectiveContext)));
    }

    private String resolveContext(String context, BotForgeUser user) {
        if (context != null && !context.isBlank()) {
            if ("global".equals(context)) {
                return DocumentService.Context.GLOBAL_CONTEXT;
            }
            return context;
        }
        return user.effectiveContext();
    }
}
