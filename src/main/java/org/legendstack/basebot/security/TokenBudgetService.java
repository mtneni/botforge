package org.legendstack.basebot.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Tracks and enforces daily token usage budgets per user.
 * <p>
 * Stores daily counters in Redis with automatic midnight expiry.
 * When a user exceeds their daily budget, chat requests should be rejected.
 */
@Service
public class TokenBudgetService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBudgetService.class);
    private static final String KEY_PREFIX = "BotForge:tokens:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${botforge.token-budget.daily-limit:100000}")
    private long dailyLimit;

    public TokenBudgetService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Record token usage for a user. Returns false if the budget is exceeded.
     */
    public boolean recordUsage(String userId, long tokensUsed) {
        String key = buildKey(userId);
        try {
            Long current = redisTemplate.opsForValue().increment(key, tokensUsed);
            if (current != null && current == tokensUsed) {
                // First usage today — set expiry at end of day
                redisTemplate.expire(key, Duration.ofDays(1));
            }

            if (current != null && current > dailyLimit) {
                logger.warn("Token budget exceeded for user {}: {}/{}", userId, current, dailyLimit);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to record token usage: {}", e.getMessage());
            // Fail open — don't block users if Redis is down
            return true;
        }
    }

    /**
     * Get remaining tokens for a user today.
     */
    public long remaining(String userId) {
        String key = buildKey(userId);
        try {
            String val = redisTemplate.opsForValue().get(key);
            long used = val != null ? Long.parseLong(val) : 0;
            return Math.max(0, dailyLimit - used);
        } catch (Exception e) {
            return dailyLimit; // Fail open
        }
    }

    /**
     * Get current usage for a user today.
     */
    public long currentUsage(String userId) {
        String key = buildKey(userId);
        try {
            String val = redisTemplate.opsForValue().get(key);
            return val != null ? Long.parseLong(val) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public long getDailyLimit() {
        return dailyLimit;
    }

    private String buildKey(String userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now().format(DATE_FMT);
    }
}
