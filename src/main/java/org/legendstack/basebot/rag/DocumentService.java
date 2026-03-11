package org.legendstack.basebot.rag;

import com.embabel.agent.filter.PropertyFilter;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.model.Chunk;
import com.embabel.agent.rag.model.ContentRoot;
import com.embabel.agent.rag.model.NavigableDocument;
import com.embabel.agent.rag.store.ChunkingContentElementRepository;
import org.legendstack.basebot.BotForgeProperties;
import org.legendstack.basebot.user.BotForgeUser;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service for managing document ingestion and retrieval.
 */
@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final ChunkingContentElementRepository contentRepository;
    private final TikaHierarchicalContentReader contentReader;
    private final BotForgeProperties properties;
    private final List<DocumentInfo> documents = new CopyOnWriteArrayList<>();

    /**
     * Summary info about an ingested document.
     */
    public record DocumentInfo(String uri, String title, String context, int chunkCount, Instant ingestedAt) {
    }

    public record Context(BotForgeUser user, String overrideContext) {

        public static final String CONTEXT_KEY = "context";

        public static final String GLOBAL_CONTEXT = "global";

        public Context(BotForgeUser user) {
            this(user, null);
        }

        public static Context global(BotForgeUser user) {
            return new Context(user, GLOBAL_CONTEXT);
        }

        public String effectiveContext() {
            return overrideContext != null ? overrideContext : user.effectiveContext();
        }

        public Map<String, Object> metadata() {
            return Map.of(
                    "ingestedBy", user.getId(),
                    CONTEXT_KEY, effectiveContext());
        }
    }

    public DocumentService(ChunkingContentElementRepository contentRepository, BotForgeProperties properties) {
        this.contentRepository = contentRepository;
        this.contentReader = new TikaHierarchicalContentReader();
        this.properties = properties;
    }

    @PostConstruct
    void loadDocumentsFromDatabase() {
        try {
            for (var root : contentRepository.findAll(ContentRoot.class)) {
                var context = (String) root.getMetadata().get(Context.CONTEXT_KEY);
                if (context == null)
                    continue;
                documents.add(new DocumentInfo(
                        root.getUri(),
                        resolveTitle(root.getTitle(), root.getUri()),
                        context,
                        0,
                        root.getIngestionTimestamp()));
            }
            logger.info("Loaded {} documents from database", documents.size());
        } catch (Exception e) {
            logger.warn("Failed to load documents from database: {}", e.getMessage());
        }
        ingestInitialDocuments();
    }

    private void ingestInitialDocuments() {
        var initialDocs = properties.initialDocuments();
        if (initialDocs == null || initialDocs.isEmpty()) {
            return;
        }

        Set<String> existingUris = documents.stream()
                .map(DocumentInfo::uri)
                .collect(Collectors.toSet());

        var systemUser = new BotForgeUser("system", "System", "system");
        var context = Context.global(systemUser);

        for (String uri : initialDocs) {
            if (existingUris.contains(uri)) {
                logger.info("Initial document already loaded, skipping: {}", uri);
                continue;
            }
            try {
                var file = new File(uri);
                if (file.exists()) {
                    ingestFile(file, context);
                } else {
                    ingestUrl(uri, context);
                }
                logger.info("Ingested initial document: {}", uri);
            } catch (Exception e) {
                logger.warn("Failed to ingest initial document {}: {}", uri, e.getMessage());
            }
        }
    }

    /**
     * Ingest a file into the RAG store.
     */
    public NavigableDocument ingestFile(File file, Context context) {
        var uri = file.toURI().toString();
        logger.info("Ingesting file: {} (URI: {})", file.getName(), uri);
        deleteDocument(uri); // Overwrite if exists
        var document = contentReader.parseFile(file, uri)
                .withMetadata(context.metadata());
        var chunkIds = contentRepository.writeAndChunkDocument(document);
        trackDocument(document, context, chunkIds.size());
        logger.info("Ingested file: {}", file.getName());
        return document;
    }

    /**
     * Ingest raw bytes into the RAG store.
     */
    public NavigableDocument ingestBytes(byte[] bytes, String uri, String filename, Context context) {
        logger.info("Ingesting bytes: {} (URI: {})", filename, uri);
        deleteDocument(uri);
        var document = contentReader.parseContent(new java.io.ByteArrayInputStream(bytes), uri)
                .withMetadata(context.metadata());
        var chunkIds = contentRepository.writeAndChunkDocument(document);
        trackDocument(document, context, chunkIds.size());
        logger.info("Ingested: {}", filename);
        return document;
    }

    /**
     * Ingest content from an input stream.
     */
    public NavigableDocument ingestStream(InputStream inputStream, String uri, String filename, Context context) {
        logger.info("Ingesting stream: {} (URI: {})", filename, uri);
        deleteDocument(uri);
        var document = contentReader.parseContent(inputStream, uri)
                .withMetadata(context.metadata());
        var chunkIds = contentRepository.writeAndChunkDocument(document);
        trackDocument(document, context, chunkIds.size());
        logger.info("Ingested: {}", filename);
        return document;
    }

    /**
     * Ingest content from a URL.
     */
    public NavigableDocument ingestUrl(String url, Context context) {
        logger.info("Ingesting URL: {}", url);
        deleteDocument(url);
        var document = contentReader.parseResource(url)
                .withMetadata(context.metadata());
        var chunkIds = contentRepository.writeAndChunkDocument(document);
        trackDocument(document, context, chunkIds.size());
        logger.info("Ingested URL: {}", url);
        return document;
    }

    public String computeHash(byte[] bytes) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        }
    }

    private void trackDocument(NavigableDocument document, Context context, int chunkCount) {
        documents.removeIf(d -> d.uri().equals(document.getUri()));
        documents.add(new DocumentInfo(
                document.getUri(),
                resolveTitle(document.getTitle(), document.getUri()),
                context.effectiveContext(),
                chunkCount,
                Instant.now()));
    }

    /**
     * Resolve a human-readable title: prefer Tika-extracted title,
     * fall back to filename extracted from the URI.
     */
    private static String resolveTitle(String tikaTitle, String uri) {
        if (tikaTitle != null && !tikaTitle.isBlank()
                && !tikaTitle.toLowerCase().contains("parse error")
                && !tikaTitle.toLowerCase().contains("unknown")
                && !tikaTitle.contains("://")) {
            return tikaTitle;
        }
        if (uri == null)
            return "Untitled";
        // Extract filename from URI (handles upload://ctx/hash/file.md,
        // file:///path/to/file.md, etc.)
        int lastSlash = uri.lastIndexOf('/');
        String filename = lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
        // Remove extension for a cleaner display name
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            filename = filename.substring(0, dot);
        }
        // Replace underscores/dashes with spaces and title-case
        return filename.replace('_', ' ').replace('-', ' ');
    }

    /**
     * Check if a document with the given URI is already ingested.
     */
    public boolean exists(String uri) {
        return documents.stream().anyMatch(d -> d.uri().equals(uri));
    }

    /**
     * Get list of all ingested documents.
     */
    public List<DocumentInfo> getDocuments() {
        return List.copyOf(documents);
    }

    /**
     * Get documents filtered by the user's effective context.
     */
    public List<DocumentInfo> getDocuments(String effectiveContext) {
        return documents.stream()
                .filter(doc -> doc.context().equals(effectiveContext))
                .toList();
    }

    /**
     * Get list of distinct contexts found in documents
     */
    public List<String> contexts() {
        return documents.stream()
                .map(DocumentInfo::context)
                .distinct()
                .toList();
    }

    /**
     * Delete a document by its URI.
     */
    public boolean deleteDocument(String uri) {
        logger.info("Deleting document request: {}", uri);
        String normalizedTarget = normalizeUri(uri);

        // First find the actual stored URI by matching normalized versions
        String actualStoredUri = documents.stream()
                .map(DocumentInfo::uri)
                .filter(storedUri -> normalizeUri(storedUri).equals(normalizedTarget))
                .findFirst()
                .orElse(uri); // Fallback to original if not in tracking list

        logger.info("Normalized delete match: target={} -> actualStoredUri={}", normalizedTarget, actualStoredUri);

        var result = contentRepository.deleteRootAndDescendants(actualStoredUri);
        if (result != null) {
            documents.removeIf(doc -> normalizeUri(doc.uri()).equals(normalizedTarget));
            return true;
        }
        return false;
    }

    /**
     * Normalize URIs to handle variations in slashes (file:/, file:///, file://)
     * and URL-encoded characters.
     */
    public static String normalizeUri(String uri) {
        if (uri == null)
            return null;
        try {
            // Unquote and normalize slashes
            String decoded = java.net.URLDecoder.decode(uri, java.nio.charset.StandardCharsets.UTF_8);
            if (decoded.startsWith("file:")) {
                // file:/C:/... OR file:///C:/... OR file:C:/...
                // Make it consistently file:/[Path]
                return "file:/" + decoded.substring(5).replace("\\", "/").replaceAll("^/+", "");
            }
            return decoded;
        } catch (Exception e) {
            return uri;
        }
    }

    /**
     * Get total document count.
     */
    public int getDocumentCount() {
        return contentRepository.info().getDocumentCount();
    }

    /**
     * Get document count for a specific context.
     */
    public int getDocumentCount(String effectiveContext) {
        return contentRepository.count(ContentRoot.class,
                new PropertyFilter.Eq(Context.CONTEXT_KEY, effectiveContext));
    }

    /**
     * Get total chunk count.
     */
    public int getChunkCount() {
        return contentRepository.info().getChunkCount();
    }

    /**
     * Get chunk count for a specific context.
     */
    public int getChunkCount(String effectiveContext) {
        return contentRepository.count(Chunk.class,
                new PropertyFilter.Eq(Context.CONTEXT_KEY, effectiveContext));
    }

}
