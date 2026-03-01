package org.legendstack.basebot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Input/output content moderation filter for enterprise deployment.
 * <ul>
 * <li><b>Input</b>: blocks prompt injection attempts and PII submission</li>
 * <li><b>Output</b>: (future) can scan LLM responses for sensitive data
 * leakage</li>
 * </ul>
 * Only applies to POST /api/chat/message/* endpoints.
 */
@Component
public class ContentModerationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ContentModerationFilter.class);

    /**
     * Patterns that indicate prompt injection attempts.
     */
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(all\\s+)?previous\\s+instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now\\s+(a|an)\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system\\s*prompt\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[INST\\]|\\[/INST\\]|<<SYS>>|<</SYS>>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</?(system|assistant|user)>", Pattern.CASE_INSENSITIVE));

    /**
     * PII patterns — Social Security Numbers, credit card numbers.
     */
    private static final List<Pattern> PII_PATTERNS = List.of(
            // SSN: 123-45-6789
            Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
            // Credit card: 16 digits with optional separators
            Pattern.compile("\\b(?:\\d{4}[- ]?){3}\\d{4}\\b"),
            // Generic email (to warn, not block)
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Only moderate chat message input
        if (!request.getRequestURI().startsWith("/api/chat/message")
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Wrap request to allow reading body multiple times
        var wrappedRequest = new CachedBodyHttpServletRequest(request);
        String body = wrappedRequest.getCachedBody();

        // Check for prompt injection
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(body).find()) {
                logger.warn("Prompt injection attempt blocked from {}: matched pattern '{}'",
                        request.getRemoteAddr(), pattern.pattern());
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Message blocked by content moderation policy\"}");
                return;
            }
        }

        // Warn on PII (log but don't block — users may need to discuss PII topics)
        for (Pattern pattern : PII_PATTERNS) {
            if (pattern.matcher(body).find()) {
                logger.warn("PII pattern detected in message from {}: '{}'",
                        request.getRemoteAddr(), pattern.pattern());
            }
        }

        filterChain.doFilter(wrappedRequest, response);
    }
}
