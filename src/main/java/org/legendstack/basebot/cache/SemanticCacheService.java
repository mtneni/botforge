package org.legendstack.basebot.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for semantic caching of AI responses using Redis.
 * In a full implementation, this would use a Vector Store for similarity
 * search.
 * For now, it provides the backbone for exact/parameterized caching.
 */
@Service
public class SemanticCacheService {

    private static final Logger logger = LoggerFactory.getLogger(SemanticCacheService.class);
    private static final String CACHE_PREFIX = "BotForge:cache:";
    private final RedisTemplate<String, String> redisTemplate;

    public SemanticCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get a cached response for a given query.
     */
    public Optional<String> get(String query, String persona) {
        String key = generateKey(query, persona);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            logger.info("Semantic cache hit for query: {}", query);
            return Optional.of(cached);
        }
        return Optional.empty();
    }

    /**
     * Store a response in the cache.
     */
    public void put(String query, String persona, String response) {
        String key = generateKey(query, persona);
        // Cache for 24 hours by default in enterprise context
        redisTemplate.opsForValue().set(key, response, 24, TimeUnit.HOURS);
        logger.debug("Stored response in semantic cache for query: {}", query);
    }

    private String generateKey(String query, String persona) {
        // In a real semantic cache, this would be a vector embedding lookup
        return CACHE_PREFIX + persona + ":" + query.trim().toLowerCase().hashCode();
    }
}
