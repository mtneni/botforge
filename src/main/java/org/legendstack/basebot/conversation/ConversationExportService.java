package org.legendstack.basebot.conversation;

import org.legendstack.basebot.user.BotForgeUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting conversations to Markdown format.
 */
@Service
public class ConversationExportService {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final BotForgeUserService userService;

    public ConversationExportService(ConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            BotForgeUserService userService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    /**
     * Export a conversation as a Markdown string.
     * Validates that the conversation belongs to the authenticated user.
     */
    @Transactional(readOnly = true)
    public String exportAsMarkdown(String conversationId) {
        var user = userService.getAuthenticatedUser();
        var conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!conversation.getUserId().equals(user.getId())) {
            throw new SecurityException("Access denied: conversation does not belong to user");
        }

        List<ChatMessageEntity> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);

        StringBuilder md = new StringBuilder();
        md.append("# ").append(conversation.getTitle()).append("\n\n");
        md.append("**Persona:** ").append(conversation.getPersona()).append("  \n");
        md.append("**Created:** ").append(TIMESTAMP_FMT.format(conversation.getCreatedAt()))
                .append(" UTC  \n");
        md.append("**Exported:** ").append(TIMESTAMP_FMT.format(java.time.Instant.now()))
                .append(" UTC  \n\n");
        md.append("---\n\n");

        for (ChatMessageEntity msg : messages) {
            String role = "user".equals(msg.getRole()) ? "🧑 **User**" : "🤖 **Assistant**";
            String time = TIMESTAMP_FMT.format(msg.getTimestamp());
            md.append(role).append(" — ").append(time).append("\n\n");
            md.append(msg.getContent()).append("\n\n");
            md.append("---\n\n");
        }

        return md.toString();
    }
}
