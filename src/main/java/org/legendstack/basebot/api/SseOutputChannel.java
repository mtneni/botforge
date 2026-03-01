package org.legendstack.basebot.api;

import com.embabel.agent.api.channel.MessageOutputChannelEvent;
import com.embabel.agent.api.channel.OutputChannel;
import com.embabel.agent.api.channel.OutputChannelEvent;
import com.embabel.agent.api.channel.ProgressOutputChannelEvent;
import com.embabel.chat.AssistantMessage;
import com.embabel.chat.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * SSE-based output channel replacing VaadinOutputChannel.
 * Routes assistant messages to a blocking queue (for the REST response)
 * and pushes progress events directly to the SSE emitter.
 */
public class SseOutputChannel implements OutputChannel {

    private static final Logger logger = LoggerFactory.getLogger(SseOutputChannel.class);

    private final BlockingQueue<Message> messageQueue;
    private final SseEmitterRegistry registry;
    private final String userId;
    private final String conversationId;

    public SseOutputChannel(BlockingQueue<Message> messageQueue, SseEmitterRegistry registry, String userId,
            String conversationId) {
        this.messageQueue = messageQueue;
        this.registry = registry;
        this.userId = userId;
        this.conversationId = conversationId;
    }

    @Override
    public void send(OutputChannelEvent event) {
        if (event instanceof MessageOutputChannelEvent msgEvent) {
            var msg = msgEvent.getMessage();
            if (msg instanceof AssistantMessage) {
                // Send the complete message immediately — no artificial token-by-token delay
                sendSseEvent("message",
                        new ChatEvent(conversationId, "assistant", msg.getContent(), new ArrayList<>()));

                // Also queue for session persistence
                messageQueue.offer(msg);
            }
        } else if (event instanceof ProgressOutputChannelEvent progressEvent) {
            sendSseEvent("progress", new ProgressEvent(conversationId, progressEvent.getMessage()));
        }
    }

    private void sendSseEvent(String eventName, Object data) {
        var userEmitters = registry.getEmitters(userId);
        if (userEmitters.isEmpty()) {
            logger.debug("No active SSE emitters for user {}", userId);
            return;
        }
        for (var emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                logger.debug("SSE send failed for an emitter of user {}: {}", userId, e.getMessage());
            }
        }
    }

    public void sendError(String errorMessage) {
        sendSseEvent("error", new ErrorEvent(conversationId, errorMessage));
    }

    public void sendMemoryEvent(String proposition, String status) {
        sendSseEvent("memory", new MemoryEvent(conversationId, proposition, status));
    }

    public record ChatEvent(String conversationId, String role, String content, List<Citation> citations) {
    }

    public record Citation(String uri, String title, String snippet) {
    }

    public record ProgressEvent(String conversationId, String message) {
    }

    public record ErrorEvent(String conversationId, String error) {
    }

    public record MemoryEvent(String conversationId, String proposition, String status) {
    }
}
