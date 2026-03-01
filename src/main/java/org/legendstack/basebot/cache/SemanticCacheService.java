package org.legendstack.basebot.cache;

import com.embabel.common.ai.model.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Semantic cache that uses embedding similarity for cache lookups.
 * <p>
 * How it works:
 * <ol>
 * <li>On {@code put()}, the query is embedded and stored alongside the response
 * in Redis.</li>
 * <li>On {@code get()}, the incoming query is embedded and compared against all
 * cached
 * entries for the same persona using cosine similarity.</li>
 * <li>If any cached entry exceeds the {@code SIMILARITY_THRESHOLD}, it's
 * returned as a hit.</li>
 * </ol>
 * <p>
 * This replaces the original exact-match cache, allowing semantically
 * equivalent
 * queries (e.g., "What's the weather?" vs "Tell me about the weather") to hit
 * cache.
 */
@Service
public class SemanticCacheService {

    private static final Logger logger = LoggerFactory.getLogger(SemanticCacheService.class);
    private static final String CACHE_PREFIX = "BotForge:cache:";
    private static final String EMBEDDING_PREFIX = "BotForge:embedding:";
    private static final double SIMILARITY_THRESHOLD = 0.92;

    private final RedisTemplate<String, String> redisTemplate;
    private final EmbeddingService embeddingService;

    public SemanticCacheService(RedisTemplate<String, String> redisTemplate,
            EmbeddingService embeddingService) {
        this.redisTemplate = redisTemplate;
        this.embeddingService = embeddingService;
    }

    /**
     * Look up a cached response by computing the embedding of the query
     * and finding the most similar cached entry above the threshold.
     */
    public Optional<String> get(String query, String persona) {
        try {
            float[] queryEmbedding = embed(query);
            String pattern = EMBEDDING_PREFIX + persona + ":*";

            // Scan cached embeddings for this persona
            var keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return Optional.empty();
            }

            String bestKey = null;
            double bestSimilarity = -1;

            for (String embKey : keys) {
                String embStr = redisTemplate.opsForValue().get(embKey);
                if (embStr == null)
                    continue;

                float[] cachedEmbedding = deserializeEmbedding(embStr);
                double similarity = cosineSimilarity(queryEmbedding, cachedEmbedding);

                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestKey = embKey;
                }
            }

            if (bestSimilarity >= SIMILARITY_THRESHOLD && bestKey != null) {
                // Derive the response key from the embedding key
                String cacheKey = bestKey.replace(EMBEDDING_PREFIX, CACHE_PREFIX);
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    logger.info("Semantic cache hit (similarity={}) for query: {}",
                            String.format("%.3f", bestSimilarity), query);
                    return Optional.of(cached);
                }
            }
        } catch (Exception e) {
            logger.warn("Semantic cache lookup failed, falling through: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Store a response with its embedding in the cache.
     */
    public void put(String query, String persona, String response) {
        try {
            float[] queryEmbedding = embed(query);
            String normalized = query.trim().toLowerCase();
            String keySuffix = persona + ":" + Integer.toHexString(normalized.hashCode());

            String cacheKey = CACHE_PREFIX + keySuffix;
            String embeddingKey = EMBEDDING_PREFIX + keySuffix;

            redisTemplate.opsForValue().set(cacheKey, response, 24, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(embeddingKey, serializeEmbedding(queryEmbedding),
                    24, TimeUnit.HOURS);

            logger.debug("Stored response in semantic cache for query: {}", query);
        } catch (Exception e) {
            logger.warn("Failed to store in semantic cache: {}", e.getMessage());
        }
    }

    private float[] embed(String text) {
        var embedding = embeddingService.embed(text);
        float[] result = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            result[i] = embedding[i];
        }
        return result;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length)
            return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    private String serializeEmbedding(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(embedding[i]);
        }
        return sb.toString();
    }

    private float[] deserializeEmbedding(String s) {
        String[] parts = s.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i]);
        }
        return result;
    }
}
