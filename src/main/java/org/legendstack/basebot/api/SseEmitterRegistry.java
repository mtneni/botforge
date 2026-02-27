package org.legendstack.basebot.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active SSE emitters by user ID, supporting multiple emitters
 * per user (e.g. multiple browser tabs).
 */
@Component
public class SseEmitterRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterRegistry.class);
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void register(String userId, SseEmitter emitter) {
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(userId, emitter);
        });
        emitter.onError(e -> remove(userId, emitter));

        logger.debug("Registered SSE emitter for user {}. Total emitters for user: {}",
                userId, emitters.get(userId).size());
    }

    public List<SseEmitter> getEmitters(String userId) {
        return emitters.getOrDefault(userId, new ArrayList<>());
    }

    public void remove(String userId, SseEmitter emitter) {
        var userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
            logger.debug("Removed SSE emitter for user {}. Remaining: {}",
                    userId, userEmitters.size());
        }
    }
}
