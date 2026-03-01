package org.legendstack.basebot.api;

import com.embabel.chat.UserMessage;
import org.legendstack.basebot.BotForgeProperties;
import org.legendstack.basebot.conversation.ConversationService;
import org.legendstack.basebot.user.BotForgeUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Map;

/**
 * Chat REST controller with SSE streaming for real-time
 * tool-call progress and assistant responses.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private static final Logger logger = LoggerFactory.getLogger(ChatRestController.class);

    private final ChatSessionManager sessionManager;
    private final BotForgeUserService userService;
    private final BotForgeProperties properties;
    private final ConversationService conversationService;
    private final SseEmitterRegistry sseRegistry;

    public ChatRestController(ChatSessionManager sessionManager,
            BotForgeUserService userService,
            BotForgeProperties properties,
            ConversationService conversationService,
            SseEmitterRegistry sseRegistry) {
        this.sessionManager = sessionManager;
        this.userService = userService;
        this.properties = properties;
        this.conversationService = conversationService;
        this.sseRegistry = sseRegistry;
    }

    public record ChatMessageRequest(String message) {
    }

    /**
     * SSE stream endpoint — client connects once, receives all events.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request) {
        var emitter = new SseEmitter(5 * 60 * 1000L); // 5 min timeout
        var user = userService.getAuthenticatedUser();

        // Register the emitter for this user session
        sseRegistry.register(user.getId(), emitter);

        emitter.onCompletion(() -> {
            logger.debug("SSE connection completed for user {}", user.getUsername());
            sseRegistry.remove(user.getId(), emitter);
        });
        emitter.onTimeout(() -> {
            logger.debug("SSE connection timed out for user {}", user.getUsername());
            emitter.complete();
            sseRegistry.remove(user.getId(), emitter);
        });
        emitter.onError(e -> {
            logger.debug("SSE connection error for user {}: {}", user.getUsername(), e.getMessage());
            sseRegistry.remove(user.getId(), emitter);
        });

        // Send initial keep-alive
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("status", "connected")));
        } catch (Exception e) {
            logger.debug("Failed to send SSE keep-alive: {}", e.getMessage());
        }

        return emitter;
    }

    @PostMapping("/message/{conversationId}")
    public ResponseEntity<Map<String, String>> sendMessage(@PathVariable String conversationId,
            @RequestBody ChatMessageRequest request) {
        var text = request.message();
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        var user = userService.getAuthenticatedUser();
        var sessionData = sessionManager.getOrCreate(conversationId, user);

        // Process message asynchronously
        Thread.startVirtualThread(() -> {
            try {
                var userMessage = new UserMessage(text, user.getDisplayName());
                logger.info("Sending message to conversation {}: {}", conversationId, text);

                // Save user message to Postgres
                conversationService.saveMessage(conversationId, userMessage);

                sessionData.chatSession().onUserMessage(userMessage);

                // Memory extraction is handled inside ChatActions.respond() — no
                // duplicate event publishing here.
            } catch (Exception e) {
                logger.error("Error processing chat message", e);
                // Notify client of the failure via SSE (#19)
                sessionData.outputChannel().sendError(e.getMessage() != null
                        ? e.getMessage()
                        : "An unexpected error occurred");
            }
        });

        return ResponseEntity.accepted().body(Map.of("status", "processing"));
    }

    @GetMapping("/history/{conversationId}")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable String conversationId) {
        return conversationService.getConversation(conversationId)
                .map(conv -> {
                    var messages = new ArrayList<Map<String, String>>();
                    for (var msg : conv.getMessages()) {
                        messages.add(Map.of(
                                "id", msg.getId(),
                                "role", msg.getRole(),
                                "content", msg.getContent(),
                                "timestamp", String.valueOf(msg.getTimestamp().toEpochMilli())));
                    }
                    return ResponseEntity.ok(Map.<String, Object>of("messages", messages, "title", conv.getTitle()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all conversations for the authenticated user.
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listConversations() {
        var user = userService.getAuthenticatedUser();
        var chats = conversationService.getUserConversations(user.getId());
        return ResponseEntity.ok(Map.of("conversations", chats));
    }

    /**
     * Create a new conversation.
     */
    @PostMapping("/new")
    public ResponseEntity<org.legendstack.basebot.conversation.Conversation> createConversation(
            @RequestBody Map<String, String> body) {
        var user = userService.getAuthenticatedUser();
        String title = body.getOrDefault("title", "New Conversation");
        String defaultPersona = user.getPersonaOverride() != null ? user.getPersonaOverride()
                : properties.chat().persona();
        String persona = body.getOrDefault("persona", defaultPersona);
        var conv = conversationService.createConversation(user, title, persona);
        return ResponseEntity.ok(conv);
    }

    /**
     * Rename the current chat session.
     */
    @PutMapping("/{conversationId}/title")
    public ResponseEntity<Map<String, String>> renameConversation(@PathVariable String conversationId,
            @RequestBody Map<String, String> body) {
        var user = userService.getAuthenticatedUser();
        return conversationService.getConversation(conversationId)
                .map(conv -> {
                    if (!conv.getUserId().equals(user.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Map<String, String>>build();
                    }
                    String newTitle = body.getOrDefault("title", "New Conversation");
                    conversationService.renameConversation(conversationId, newTitle);
                    return ResponseEntity.ok(Map.of("status", "renamed"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Clear the current chat session.
     */
    @DeleteMapping("/session")
    public ResponseEntity<Map<String, String>> clearSession(HttpServletRequest request) {
        sessionManager.remove(request.getSession().getId());
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Map<String, String>> deleteConversation(@PathVariable String conversationId) {
        var user = userService.getAuthenticatedUser();
        return conversationService.getConversation(conversationId)
                .map(conv -> {
                    if (!conv.getUserId().equals(user.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Map<String, String>>build();
                    }
                    conversationService.deleteConversation(conversationId);
                    sessionManager.remove(conversationId);
                    return ResponseEntity.ok(Map.of("status", "deleted"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
