package org.legendstack.basebot;

import org.legendstack.basebot.cache.SemanticCacheService;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.reference.LlmReference;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.rag.service.SearchOperations;
import com.embabel.chat.Conversation;
import com.embabel.chat.SimpleMessageFormatter;
import com.embabel.chat.UserMessage;
import com.embabel.chat.WindowingConversationFormatter;
import com.embabel.common.core.types.Named;
import com.embabel.dice.agent.Memory;
import com.embabel.dice.projection.memory.MemoryProjector;
import com.embabel.dice.proposition.PropositionRepository;
import org.legendstack.basebot.event.ConversationAnalysisRequestEvent;
import org.legendstack.basebot.user.BotForgeUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.legendstack.basebot.api.ChatSessionManager;
import org.legendstack.basebot.conversation.ConversationService;

/**
 * The platform can use any action to respond to user messages.
 * Picks up references and tools configured as Spring beans.
 * Thus extensibility works via profile — simply add beans
 * under com.embabel.bot
 */
@EmbabelComponent
public class ChatActions {

    private final Logger logger = LoggerFactory.getLogger(ChatActions.class);

    private final SearchOperations searchOperations;
    private final BotForgeProperties properties;
    private final List<LlmReference> globalReferences;
    private final List<Tool> globalTools;
    private final MemoryProjector memoryProjector;
    private final PropositionRepository propositionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SemanticCacheService semanticCacheService;
    private final ChatSessionManager chatSessionManager;
    private final ConversationService conversationService;
    private final PersonaRegistry personaRegistry;
    private final OrchestratorService orchestratorService;

    public ChatActions(
            SearchOperations searchOperations,
            List<LlmReference> globalReferences,
            List<Tool> globalTools,
            BotForgeProperties properties,
            MemoryProjector memoryProjector,
            PropositionRepository propositionRepository,
            ApplicationEventPublisher eventPublisher,
            SemanticCacheService semanticCacheService,
            ChatSessionManager chatSessionManager,
            ConversationService conversationService,
            PersonaRegistry personaRegistry,
            OrchestratorService orchestratorService) {
        this.searchOperations = searchOperations;
        this.globalReferences = globalReferences;
        this.globalTools = globalTools;
        this.properties = properties;
        this.memoryProjector = memoryProjector;
        this.propositionRepository = propositionRepository;
        this.eventPublisher = eventPublisher;
        this.semanticCacheService = semanticCacheService;
        this.chatSessionManager = chatSessionManager;
        this.conversationService = conversationService;
        this.personaRegistry = personaRegistry;
        this.orchestratorService = orchestratorService;

        logger.info("ChatActions initialized. Global references: [{}], Global tools: [{}]",
                globalReferences.stream().map(Named::getName).collect(Collectors.joining(", ")),
                globalTools.stream().map(t -> t.getDefinition().getName()).collect(Collectors.joining(", ")));
    }

    /**
     * Bind user to AgentProcess. Will run once at the start of the process.
     */
    @Action
    BotForgeUser bindUser(OperationContext context) {
        var forUser = context.getProcessContext().getProcessOptions().getIdentities().getForUser();
        if (forUser instanceof BotForgeUser su) {
            return su;
        } else {
            logger.warn("bindUser: forUser is not a BotForgeUser: {}", forUser);
            return null;
        }
    }

    @Action(canRerun = true, trigger = UserMessage.class)
    void respond(
            Conversation conversation,
            BotForgeUser user,
            ActionContext context) {

        String conversationId = chatSessionManager.findIdByConversation(conversation);
        var effectiveProperties = buildEffectiveProperties(conversationId);

        var recentContextStr = new WindowingConversationFormatter(
                SimpleMessageFormatter.INSTANCE).format(conversation.last(properties.chat().messagesToEmbed()));

        var messages = conversation.getMessages();
        var userPrompt = messages.get(messages.size() - 1).getContent();

        // --- Orchestrator delegation ---
        String workingMemory = "";
        if ("orchestrator".equals(effectiveProperties.chat().persona())) {
            logger.info("Orchestrator active. Deciding sub-agent plan...");
            var result = orchestratorService.execute(effectiveProperties, user, recentContextStr, userPrompt, context);
            workingMemory = result.workingMemory();
            effectiveProperties = resolvePersonaProperties(effectiveProperties, result.finalPersonaId());
        }

        // --- Semantic cache check ---
        var cached = semanticCacheService.get(recentContextStr, effectiveProperties.chat().persona());
        if (cached.isPresent()) {
            logger.info("Serving cached response for: {}", userPrompt);
            var msg = conversation.addMessage(new com.embabel.chat.AssistantMessage(cached.get()));
            if (conversationId != null) {
                conversationService.saveMessage(conversationId, msg);
            }
            context.sendMessage(msg);
            return;
        }

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

        // Store in cache
        semanticCacheService.put(recentContextStr, effectiveProperties.chat().persona(),
                assistantMessage.getContent());

        if (properties.memory().enabled()) {
            eventPublisher.publishEvent(new ConversationAnalysisRequestEvent(this, user, conversation));
        }
    }

    /**
     * Build effective properties by resolving the persona from the conversation
     * entity.
     */
    private BotForgeProperties buildEffectiveProperties(String conversationId) {
        if (conversationId == null) {
            return properties;
        }
        var dbConvOpt = conversationService.getConversation(conversationId);
        if (dbConvOpt.isEmpty()) {
            return properties;
        }

        String convPersonaId = dbConvOpt.get().getPersona();
        return resolvePersonaProperties(properties, convPersonaId);
    }

    /**
     * Create a copy of properties with persona-specific overrides applied.
     */
    private BotForgeProperties resolvePersonaProperties(BotForgeProperties baseProps, String personaId) {
        var resolved = personaRegistry.resolve(personaId);
        if (resolved.isEmpty()) {
            return baseProps;
        }
        var p = resolved.get();
        var newChat = new ChatbotOptions(
                baseProps.chat().llm(), baseProps.chat().messagesToEmbed(), p.objective(), p.behaviour(), personaId,
                baseProps.chat().maxWords(), baseProps.chat().memoryEagerLimit(), baseProps.chat().warmth(),
                baseProps.chat().memoryVerbosity(),
                baseProps.chat().showPrompts(), baseProps.chat().showResponses(), baseProps.chat().tagline());
        return baseProps.withChat(newChat);
    }
}
