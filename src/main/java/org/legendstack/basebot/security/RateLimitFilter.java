package org.legendstack.basebot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple sliding-window rate limiter for chat message endpoints.
 * <p>
 * Limits requests per user to {@code MAX_REQUESTS} within a {@code WINDOW_MS}
 * window.
 * Only applies to POST /api/chat/message/* endpoints.
 * <p>
 * For production, consider replacing with a Redis-backed limiter (e.g.,
 * Bucket4j + Redis)
 * for horizontal scaling across multiple instances.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    /**
     * Maximum chat messages per user per window.
     */
    private static final int MAX_REQUESTS = 30;

    /**
     * Window duration in milliseconds (1 minute).
     */
    private static final long WINDOW_MS = 60_000;

    private final Map<String, UserRateState> userStates = new ConcurrentHashMap<>();

    private static class UserRateState {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();

        boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                // Reset the window
                synchronized (this) {
                    if (now - windowStart > WINDOW_MS) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet() <= MAX_REQUESTS;
        }

        int remaining() {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                return MAX_REQUESTS;
            }
            return Math.max(0, MAX_REQUESTS - count.get());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Only rate-limit chat message POSTs
        if (!uri.startsWith("/api/chat/message") || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = resolveUserId(request);
        UserRateState state = userStates.computeIfAbsent(userId, k -> new UserRateState());

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(state.remaining()));

        if (!state.tryAcquire()) {
            logger.warn("Rate limit exceeded for user: {}", userId);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Max "
                    + MAX_REQUESTS + " messages per minute.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveUserId(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            return session.getId();
        }
        // Fallback to IP for unauthenticated requests
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
