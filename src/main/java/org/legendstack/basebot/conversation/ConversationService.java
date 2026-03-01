package org.legendstack.basebot.conversation;

import com.embabel.chat.Message;
import com.embabel.chat.UserMessage;
import org.legendstack.basebot.user.BotForgeUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository,
            ChatMessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Conversation createConversation(BotForgeUser user, String title, String persona) {
        Conversation conversation = new Conversation(title, user.getId(), persona);
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(String userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> getConversation(String conversationId) {
        return conversationRepository.findById(conversationId);
    }

    @Transactional
    public void saveMessage(String conversationId, Message message) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            String role = (message instanceof UserMessage) ? "user" : "assistant";
            ChatMessageEntity entity = new ChatMessageEntity(role, message.getContent());

            // Citations are not natively available on AssistantMessage in this version
            // Removed TO-DO block

            conv.addMessage(entity);
            conversationRepository.save(conv);
            logger.debug("Saved {} message to conversation {}", role, conversationId);
        });
    }

    @Transactional
    public void renameConversation(String conversationId, String newTitle) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            conv.setTitle(newTitle);
            conversationRepository.save(conv);
            logger.debug("Renamed conversation {}", conversationId);
        });
    }

    @Transactional
    public void deleteConversation(String conversationId) {
        conversationRepository.deleteById(conversationId);
    }
}
