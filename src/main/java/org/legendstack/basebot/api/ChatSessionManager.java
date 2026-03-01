package org.legendstack.basebot.api;

import com.embabel.chat.AssistantMessage;
import com.embabel.chat.ChatSession;
import com.embabel.chat.Chatbot;
import com.embabel.chat.Conversation;
import com.embabel.chat.Message;
import com.embabel.chat.UserMessage;
import org.legendstack.basebot.conversation.ConversationService;
import org.legendstack.basebot.user.BotForgeUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chat sessions per conversation, with O(1) reverse lookup.
 */
@Component
public class ChatSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(ChatSessionManager.class);

    private final Chatbot chatbot;
    private final ConversationService conversationService;
    private final SseEmitterRegistry sseRegistry;
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
    private final Map<Conversation, String> conversationToId = new ConcurrentHashMap<>();

    public ChatSessionManager(Chatbot chatbot, ConversationService conversationService,
            SseEmitterRegistry sseRegistry) {
        this.chatbot = chatbot;
        this.conversationService = conversationService;
        this.sseRegistry = sseRegistry;
    }

    public static class SessionData {
        private final ChatSession chatSession;
        private final BlockingQueue<Message> responseQueue;
        private final SseOutputChannel outputChannel;

        public SessionData(ChatSession chatSession, BlockingQueue<Message> responseQueue,
                SseOutputChannel outputChannel) {
            this.chatSession = chatSession;
            this.responseQueue = responseQueue;
            this.outputChannel = outputChannel;
        }

        public ChatSession chatSession() {
            return chatSession;
        }

        public BlockingQueue<Message> responseQueue() {
            return responseQueue;
        }

        public SseOutputChannel outputChannel() {
            return outputChannel;
        }
    }

    public SessionData getOrCreate(String conversationId, BotForgeUser user) {
        return sessions.computeIfAbsent(conversationId, id -> {
            var responseQueue = new ArrayBlockingQueue<Message>(10);
            var outputChannel = new SseOutputChannel(responseQueue, sseRegistry, user.getId(), conversationId);

            // Create the AI session
            var chatSession = chatbot.createSession(user, outputChannel, null, null);

            // Load history from Postgres if it exists
            conversationService.getConversation(conversationId).ifPresent(pConv -> {
                logger.info("Loading {} historical messages for conversation {}", pConv.getMessages().size(),
                        conversationId);
                var eConv = chatSession.getConversation();
                for (var msg : pConv.getMessages()) {
                    if ("user".equals(msg.getRole())) {
                        eConv.addMessage(new UserMessage(msg.getContent(), user.getDisplayName()));
                    } else if ("assistant".equals(msg.getRole())) {
                        eConv.addMessage(new AssistantMessage(msg.getContent()));
                    }
                }
            });

            // Register reverse lookup
            conversationToId.put(chatSession.getConversation(), conversationId);

            logger.info("Initialized chat session for user {} (Conversation: {})", user.getUsername(), id);
            return new SessionData(chatSession, responseQueue, outputChannel);
        });
    }

    /**
     * O(1) reverse lookup from Conversation object to conversation ID.
     */
    public String findIdByConversation(Conversation conversation) {
        return conversationToId.get(conversation);
    }

    public SessionData get(String httpSessionId) {
        return sessions.get(httpSessionId);
    }

    public void remove(String conversationId) {
        var removed = sessions.remove(conversationId);
        if (removed != null) {
            conversationToId.remove(removed.chatSession().getConversation());
        }
        logger.info("Removed chat session for conversation: {}", conversationId);
    }
}
