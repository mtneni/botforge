package org.legendstack.basebot;

import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.reference.LlmReference;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.rag.service.SearchOperations;
import com.embabel.chat.Conversation;
import com.embabel.dice.agent.Memory;
import com.embabel.dice.projection.memory.MemoryProjector;
import com.embabel.dice.proposition.PropositionRepository;
import io.micrometer.core.instrument.Timer;
import org.legendstack.basebot.audit.AuditService;
import org.legendstack.basebot.cache.SemanticCacheService;
import org.legendstack.basebot.conversation.ConversationService;
import org.legendstack.basebot.event.ConversationAnalysisRequestEvent;
import org.legendstack.basebot.observability.BotForgeMetrics;
import org.legendstack.basebot.security.TokenBudgetService;
import org.legendstack.basebot.user.BotForgeUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class ResponsePipeline {

    private final Logger logger = LoggerFactory.getLogger(ResponsePipeline.class);

    private final SearchOperations searchOperations;
    private final BotForgeProperties properties;
    private final List<LlmReference> globalReferences;
    private final List<Tool> globalTools;
    private final MemoryProjector memoryProjector;
    private final PropositionRepository propositionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SemanticCacheService semanticCacheService;
    private final ConversationService conversationService;
    private final BotForgeMetrics metrics;
    private final AuditService auditService;
    private final TokenBudgetService tokenBudgetService;

    public ResponsePipeline(
            SearchOperations searchOperations,
            List<LlmReference> globalReferences,
            List<Tool> globalTools,
            BotForgeProperties properties,
            MemoryProjector memoryProjector,
            PropositionRepository propositionRepository,
            ApplicationEventPublisher eventPublisher,
            SemanticCacheService semanticCacheService,
            ConversationService conversationService,
            BotForgeMetrics metrics,
            AuditService auditService,
            TokenBudgetService tokenBudgetService) {
        this.searchOperations = searchOperations;
        this.globalReferences = globalReferences;
        this.globalTools = globalTools;
        this.properties = properties;
        this.memoryProjector = memoryProjector;
        this.propositionRepository = propositionRepository;
        this.eventPublisher = eventPublisher;
        this.semanticCacheService = semanticCacheService;
        this.conversationService = conversationService;
        this.metrics = metrics;
        this.auditService = auditService;
        this.tokenBudgetService = tokenBudgetService;
    }

    public void execute(
            BotForgeProperties effectiveProperties,
            BotForgeUser user,
            ActionContext context,
            Conversation conversation,
            String conversationId,
            String userPrompt,
            String recentContextStr,
            String workingMemory,
            Timer.Sample chatTimerSample) {

        // --- Semantic cache check ---
        var cached = semanticCacheService.get(recentContextStr, effectiveProperties.chat().persona());
        if (cached.isPresent()) {
            metrics.recordCacheHit();
            logger.info("Serving cached response for: {}", userPrompt);
            var msg = conversation.addMessage(new com.embabel.chat.AssistantMessage(cached.get()));
            if (conversationId != null) {
                conversationService.saveMessage(conversationId, msg);
            }
            context.sendMessage(msg);
            metrics.recordChatResponse();
            metrics.stopChatTimer(chatTimerSample);
            return;
        }

        // --- Token Budget & Audit Logging ---
        int estTokens = Math.max(100, userPrompt.length() / 4 + 100);
        if (!tokenBudgetService.recordUsage(user.getId(), estTokens)) {
            logger.warn("User {} exceeded token budget.", user.getId());
            var msg = conversation.addMessage(new com.embabel.chat.AssistantMessage(
                    "You have exceeded your daily limit for bot interactions. Please try again tomorrow."));
            if (conversationId != null) {
                conversationService.saveMessage(conversationId, msg);
            }
            context.sendMessage(msg);
            metrics.stopChatTimer(chatTimerSample);
            return;
        }

        auditService.log(user, AuditService.ACTION_CHAT_MESSAGE,
                "Prompt length: " + userPrompt.length() + " chars, Est. LLM tokens: " + estTokens);

        // --- Assemble RAG references and tools ---
        var tools = new LinkedList<>(globalTools);
        var references = new LinkedList<>(globalReferences);
        references.add(user.personalDocs(searchOperations));
        if (properties.memory().enabled()) {
            var memory = Memory.forContext(user.currentContext())
                    .withRepository(propositionRepository)
                    .withProjector(memoryProjector)
                    .withEagerSearchAbout(recentContextStr, properties.chat().memoryEagerLimit());
            references.add(memory);
            tools.add(memory);
        }

        // --- LLM call ---
        var assistantMessage = context.ai()
                .withLlm(effectiveProperties.chat().llm())
                .withId("chat_response")
                .withTools(tools)
                .withReferences(references)
                .rendering("botforge")
                .respondWithSystemPrompt(conversation, Map.of(
                        "properties", effectiveProperties,
                        "user", user,
                        "working_memory", workingMemory));

        var msg = conversation.addMessage(assistantMessage);

        if (conversationId != null) {
            conversationService.saveMessage(conversationId, msg);
        }

        context.sendMessage(msg);
        metrics.recordChatResponse();
        metrics.stopChatTimer(chatTimerSample);

        // Store in cache
        semanticCacheService.put(recentContextStr, effectiveProperties.chat().persona(),
                assistantMessage.getContent());

        if (properties.memory().enabled()) {
            eventPublisher.publishEvent(new ConversationAnalysisRequestEvent(this, user, conversation));
        }
    }
}
