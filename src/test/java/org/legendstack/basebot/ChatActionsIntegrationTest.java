package org.legendstack.basebot;

import org.legendstack.basebot.cache.SemanticCacheService;
import org.legendstack.basebot.observability.BotForgeMetrics;
import org.legendstack.basebot.audit.AuditService;
import org.legendstack.basebot.security.TokenBudgetService;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.chat.AssistantMessage;
import com.embabel.chat.UserMessage;
import com.embabel.chat.Conversation;
import org.legendstack.basebot.conversation.ConversationService;
import org.legendstack.basebot.user.BotForgeUser;
import org.legendstack.basebot.api.ChatSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ChatActionsIntegrationTest {

    private ChatActions chatActions;

    @BeforeEach
    public void setup() {
        chatActions = new ChatActions(
                searchOperations,
                new ArrayList<>(),
                new ArrayList<>(),
                botForgeProperties,
                memoryProjector,
                propositionRepository,
                eventPublisher,
                semanticCacheService,
                chatSessionManager,
                conversationService,
                personaRegistry,
                orchestratorService,
                botForgeMetrics,
                auditService,
                tokenBudgetService);
    }

    @Mock
    private BotForgeMetrics botForgeMetrics;

    @Mock
    private AuditService auditService;

    @Mock
    private TokenBudgetService tokenBudgetService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private ChatSessionManager chatSessionManager;

    @Mock
    private SemanticCacheService semanticCacheService;

    @Mock(name = "searchOperations")
    private com.embabel.agent.rag.service.SearchOperations searchOperations;

    @Mock
    private com.embabel.dice.projection.memory.MemoryProjector memoryProjector;

    @Mock
    private com.embabel.dice.proposition.PropositionRepository propositionRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private BotForgeProperties botForgeProperties;

    @Mock
    private PersonaRegistry personaRegistry;

    @Mock
    private OrchestratorService orchestratorService;

    @Test
    public void testSendMessageSuccessfullyResponds() {
        BotForgeUser user = new BotForgeUser("user123", "Test User", "testuser");

        Conversation conversation = mock(Conversation.class);
        List<com.embabel.chat.Message> messages = new ArrayList<>();
        messages.add(new UserMessage("Hello, BotForge!"));
        when(conversation.getMessages()).thenReturn(messages);
        when(conversation.last(anyInt())).thenReturn(conversation);
        when(conversation.addMessage(any())).thenAnswer(inv -> {
            messages.add(inv.getArgument(0));
            return inv.getArgument(0);
        });

        when(chatSessionManager.findIdByConversation(conversation)).thenReturn("conv123");
        conversation.addMessage(new UserMessage("Hello, BotForge!"));

        org.legendstack.basebot.conversation.Conversation dbConv = new org.legendstack.basebot.conversation.Conversation(
                "Test",
                "user123", "assistant");
        when(conversationService.getConversation("conv123")).thenReturn(Optional.of(dbConv));

        // Use RETURNS_DEEP_STUBS to avoid ambiguous compilation errors when mocking the
        // builder chain
        ActionContext mockContext = mock(ActionContext.class, RETURNS_DEEP_STUBS);

        assertDoesNotThrow(() -> {
            chatActions.respond(conversation, user, mockContext);
        });

        verify(conversationService, times(1)).saveMessage(eq("conv123"), any(AssistantMessage.class));
        verify(mockContext, times(1)).sendMessage(any(AssistantMessage.class));
        verify(conversation, times(1)).addMessage(any(AssistantMessage.class));
    }
}
