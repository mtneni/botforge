package org.legendstack.basebot.api;

import com.embabel.chat.AssistantMessage;
import com.embabel.chat.ChatSession;
import com.embabel.chat.Chatbot;
import com.embabel.chat.UserMessage;
import com.embabel.chat.Message;
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
 * Manages chat sessions per HTTP session, replacing VaadinSession-based session
 * storage.
 */
@Component
public class ChatSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(ChatSessionManager.class);

    private final Chatbot chatbot;
    private final ConversationService conversationService;
    private final SseEmitterRegistry sseRegistry;
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

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
        private volatile String personaOverride;
        private volatile String objectiveOverride;
        private volatile String behaviourOverride;

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

        public String getPersonaOverride() {
            return personaOverride;
        }

        public void setPersonaOverride(String p) {
            this.personaOverride = p;
        }

        public String getObjectiveOverride() {
            return objectiveOverride;
        }

        public void setObjectiveOverride(String o) {
            this.objectiveOverride = o;
        }

        public String getBehaviourOverride() {
            return behaviourOverride;
        }

        public void setBehaviourOverride(String b) {
            this.behaviourOverride = b;
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

            logger.info("Initialized chat session for user {} (Conversation: {})", user.getUsername(), id);
            return new SessionData(chatSession, responseQueue, outputChannel);
        });
    }

    public String findIdByConversation(com.embabel.chat.Conversation conversation) {
        return sessions.entrySet().stream()
                .filter(e -> e.getValue().chatSession().getConversation().equals(conversation))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public SessionData get(String httpSessionId) {
        return sessions.get(httpSessionId);
    }

    public void remove(String conversationId) {
        sessions.remove(conversationId);
        logger.info("Removed chat session for conversation: {}", conversationId);
    }
}
