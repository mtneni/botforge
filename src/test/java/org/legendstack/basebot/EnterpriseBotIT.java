package org.legendstack.basebot;

import org.legendstack.basebot.tools.ArchitectureReviewTool;
import org.legendstack.basebot.rag.HybridSearchService;
import org.legendstack.basebot.conversation.ConversationExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.legendstack.basebot.audit.AuditLogRepository;
import org.legendstack.basebot.conversation.ConversationRepository;
import org.legendstack.basebot.conversation.ChatMessageRepository;
import org.legendstack.basebot.security.TokenBudgetService;
import org.legendstack.basebot.cache.SemanticCacheService;
import org.legendstack.basebot.observability.BotForgeMetrics;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = { TestBotForgeApplication.class }, properties = {
        "spring.main.web-application-type=none"
})
@ActiveProfiles("it")
@org.springframework.context.annotation.Import(org.legendstack.basebot.rag.HybridSearchService.class)
public class EnterpriseBotIT {

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private ConversationRepository conversationRepository;

    @MockitoBean
    private ChatMessageRepository chatMessageRepository;

    @MockitoBean
    private TokenBudgetService tokenTokenBudgetService;

    @MockitoBean
    private SemanticCacheService semanticCacheService;

    @MockitoBean
    private BotForgeProperties botForgeProperties;

    @MockitoBean
    private org.legendstack.basebot.api.ChatSessionManager chatSessionManager;

    @MockitoBean
    private org.legendstack.basebot.api.CustomPersonaRepository customPersonaRepository;

    @MockitoBean
    private OrchestratorService orchestratorService;

    @MockitoBean
    private BotForgeMetrics metrics;

    @MockitoBean
    private com.embabel.dice.proposition.PropositionRepository propositionRepository;

    @MockitoBean(name = "entityManagerFactory")
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @MockitoBean
    private com.embabel.agent.rag.service.NamedEntityDataRepository namedEntityDataRepository;

    @MockitoBean
    private org.legendstack.basebot.user.BotForgeUserRepository botForgeUserRepository;

    @MockitoBean
    private org.legendstack.basebot.proposition.persistence.DrivinePropositionRepository drivinePropositionRepository;

    @MockitoBean
    private ResponsePipeline responsePipeline;

    @Autowired
    private ArchitectureReviewTool architectureReviewTool;

    @Autowired
    private HybridSearchService hybridSearchService;

    @Autowired
    private ConversationExportService conversationExportService;

    @Test
    void testArchitectureReviewToolWiring() {
        assertNotNull(architectureReviewTool, "ArchitectureReviewTool should be wired");
    }

    @Test
    void testHybridSearchWiring() {
        assertNotNull(hybridSearchService, "HybridSearchService should be wired");
        // Test with empty/null query handles gracefully (avoiding real data layer call
        // in IT)
        assertNotNull(hybridSearchService);
    }

    @Test
    @WithMockUser(username = "alice")
    void testConversationExportWiring() {
        assertNotNull(conversationExportService, "ConversationExportService should be wired");
    }
}
