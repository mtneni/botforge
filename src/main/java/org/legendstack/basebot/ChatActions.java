package org.legendstack.basebot;

import org.legendstack.basebot.api.PersonaController;
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
import org.legendstack.basebot.api.ChatSessionManager;
import org.legendstack.basebot.conversation.ConversationService;
import org.legendstack.basebot.api.CustomPersonaRepository;

/**
 * The platform can use any action to respond to user messages.
 * Picks up references and tools configured as Spring beans.
 * Thus extensibility works via profile--simply add beans
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
    private final CustomPersonaRepository customPersonaRepository;

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
            CustomPersonaRepository customPersonaRepository) {
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
        this.customPersonaRepository = customPersonaRepository;

        logger.info("ChatActions initialized. Global references: [{}], Global tools: [{}]",
                globalReferences.stream().map(Named::getName).collect(java.util.stream.Collectors.joining(", ")),
                globalTools.stream().map(t -> t.getDefinition().getName())
                        .collect(java.util.stream.Collectors.joining(", ")));
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
            logger.warn("bindUser: forUser is not an BotForgeUser: {}", forUser);
            return null;
        }
    }

    @Action(canRerun = true, trigger = UserMessage.class)
    void respond(
            Conversation conversation,
            BotForgeUser user,
            ActionContext context) {

        String conversationId = chatSessionManager.findIdByConversation(conversation);
        var effectiveProperties = buildEffectiveProperties(user, conversationId);

        var recentContextStr = new WindowingConversationFormatter(
                SimpleMessageFormatter.INSTANCE).format(conversation.last(properties.chat().messagesToEmbed()));

        var messages = conversation.getMessages();
        var userPrompt = messages.get(messages.size() - 1).getContent();

        if ("orchestrator".equals(effectiveProperties.chat().persona())) {
            logger.info("Orchestrator active. Deciding sub-agent...");
            try {
                String prompt = """
                        Analyze the user's latest request and choose the best specialized persona.
                        Available personas:
                        - 'developer': for writing code, debugging, or technical architecture.
                        - 'security': for code audits, vulnerabilities, and compliance.
                        - 'astrid': for creative writing, ui design suggestions, or astrology.
                        - 'assistant': for general knowledge, data retrieval, or anything else.

                        Recent Context:
                        %s

                        User Message:
                        %s
                        """.formatted(recentContextStr, userPrompt);

                OrchestrationDecision decision = context.ai()
                        .withLlm(effectiveProperties.chat().llm())
                        .createObject(prompt, OrchestrationDecision.class);

                String chosenPersona = decision.personaId();
                if (chosenPersona == null || chosenPersona.isBlank()
                        || chosenPersona.equalsIgnoreCase("orchestrator")) {
                    chosenPersona = "assistant";
                }
                logger.info("Orchestrator selected persona: {}", chosenPersona);

                // Override the effective persona
                effectiveProperties = overwritePersona(effectiveProperties, chosenPersona);
            } catch (Exception e) {
                logger.error("Orchestrator failed to decide, falling back to assistant", e);
                effectiveProperties = overwritePersona(effectiveProperties, "assistant");
            }
        }

        // Semantic Cache Check uses actual recent context state
        java.util.Optional<String> cached = semanticCacheService.get(recentContextStr,
                effectiveProperties.chat().persona());
        if (cached.isPresent()) {
            logger.info("Serving cached response for: {}", userPrompt);
            var msg = conversation.addMessage(new com.embabel.chat.AssistantMessage(cached.get()));
            if (conversationId != null) {
                conversationService.saveMessage(conversationId, msg);
            }
            context.sendMessage(msg);
            return;
        }

        var recentContext = recentContextStr;

        var tools = new LinkedList<>(globalTools);

        var references = new LinkedList<>(globalReferences);
        references.add(user.personalDocs(searchOperations));
        if (properties.memory().enabled()) {
            var memory = Memory.forContext(user.currentContext())
                    .withRepository(propositionRepository)
                    .withProjector(memoryProjector)
                    .withEagerSearchAbout(recentContext, properties.chat().memoryEagerLimit());
            references.add(memory);
            // Also expose memory as a callable tool so the LLM can actively search
            // with its own keywords (eager search alone misses open-ended questions)
            tools.add(memory);
        }

        // Build effective properties — apply persona overrides if set
        // (already fetched at the top of respond)

        var assistantMessage = context.ai()
                .withLlm(effectiveProperties.chat().llm())
                .withId("chat_response")
                .withTools(tools)
                .withReferences(references)
                .rendering("botforge")
                .respondWithSystemPrompt(conversation, Map.of(
                        "properties", effectiveProperties,
                        "user", user));

        var msg = conversation.addMessage(assistantMessage);

        if (conversationId != null) {
            conversationService.saveMessage(conversationId, msg);
        }

        context.sendMessage(msg);

        // Store in cache with semantic context
        semanticCacheService.put(recentContextStr, effectiveProperties.chat().persona(),
                assistantMessage.getContent());

        if (properties.memory().enabled()) {
            eventPublisher.publishEvent(new ConversationAnalysisRequestEvent(this, user, conversation));
        }
    }

    /**
     * Build effective properties by applying persona overrides from the user.
     * Falls back to application.yml defaults if no overrides are set.
     */
    private BotForgeProperties buildEffectiveProperties(BotForgeUser user, String conversationId) {
        var chat = properties.chat();
        String persona = chat.persona();
        String objective = chat.objective();
        String behaviour = chat.behaviour();

        if (conversationId != null) {
            var dbConvOpt = conversationService.getConversation(conversationId);
            if (dbConvOpt.isPresent()) {
                String convPersonaId = dbConvOpt.get().getPersona();
                var presetOpt = PersonaController.PRESETS.stream()
                        .filter(p -> p.id().equals(convPersonaId))
                        .findFirst();
                if (presetOpt.isPresent()) {
                    persona = presetOpt.get().id();
                    objective = presetOpt.get().objective();
                    behaviour = presetOpt.get().behaviour();
                } else {
                    var customOpt = customPersonaRepository.findById(convPersonaId);
                    if (customOpt.isPresent()) {
                        persona = customOpt.get().getId();
                        objective = customOpt.get().getObjective();
                        behaviour = customOpt.get().getBehaviour();
                    }
                }
            }
        }

        if (persona.equals(chat.persona()) && objective.equals(chat.objective())
                && behaviour.equals(chat.behaviour())) {
            return properties; // No overrides — use original
        }

        var overriddenChat = new ChatbotOptions(
                chat.llm(), chat.messagesToEmbed(), objective, behaviour, persona,
                chat.maxWords(), chat.memoryEagerLimit(), chat.warmth(), chat.memoryVerbosity(),
                chat.showPrompts(), chat.showResponses(), chat.tagline());

        return new BotForgeProperties(overriddenChat, properties.ingestion(),
                properties.neoRag(), properties.memory(), properties.botPackages(),
                properties.initialDocuments(), properties.stylesheet(), properties.mcpToolsDescription());
    }

    private BotForgeProperties overwritePersona(BotForgeProperties baseProps, String targetPersonaId) {
        String newObjective = baseProps.chat().objective();
        String newBehaviour = baseProps.chat().behaviour();

        var presetOpt = PersonaController.PRESETS.stream()
                .filter(p -> p.id().equals(targetPersonaId))
                .findFirst();
        if (presetOpt.isPresent()) {
            newObjective = presetOpt.get().objective();
            newBehaviour = presetOpt.get().behaviour();
        } else {
            var customOpt = customPersonaRepository.findById(targetPersonaId);
            if (customOpt.isPresent()) {
                newObjective = customOpt.get().getObjective();
                newBehaviour = customOpt.get().getBehaviour();
            }
        }

        var newChat = new ChatbotOptions(
                baseProps.chat().llm(), baseProps.chat().messagesToEmbed(), newObjective, newBehaviour, targetPersonaId,
                baseProps.chat().maxWords(), baseProps.chat().memoryEagerLimit(), baseProps.chat().warmth(),
                baseProps.chat().memoryVerbosity(),
                baseProps.chat().showPrompts(), baseProps.chat().showResponses(), baseProps.chat().tagline());

        return new BotForgeProperties(newChat, baseProps.ingestion(),
                baseProps.neoRag(), baseProps.memory(), baseProps.botPackages(),
                baseProps.initialDocuments(), baseProps.stylesheet(), baseProps.mcpToolsDescription());
    }

    @com.fasterxml.jackson.annotation.JsonClassDescription("Decision on which persona should handle the user request")
    public record OrchestrationDecision(
            @com.fasterxml.jackson.annotation.JsonPropertyDescription("The ID of the persona best suited for the task. Use 'developer' for coding, 'security' for auditing, 'astrid' for astrology/creative, and 'assistant' for general knowledge or if unsure.") String personaId) {
    }
}
