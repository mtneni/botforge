package org.legendstack.basebot;

import org.legendstack.basebot.observability.BotForgeMetrics;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.chat.Conversation;
import com.embabel.chat.SimpleMessageFormatter;
import com.embabel.chat.UserMessage;
import com.embabel.chat.WindowingConversationFormatter;
import org.legendstack.basebot.user.BotForgeUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.legendstack.basebot.api.ChatSessionManager;
import org.legendstack.basebot.conversation.ConversationService;

/**
 * The platform can use any action to respond to user messages.
 * Picks up references and tools configured as Spring beans.
 * Thus extensibility works via profile — simply add beans
 * under org.legendstack.bot
 */
@EmbabelComponent
public class ChatActions {

    private final Logger logger = LoggerFactory.getLogger(ChatActions.class);

    private final BotForgeProperties properties;
    private final ChatSessionManager chatSessionManager;
    private final ConversationService conversationService;
    private final PersonaRegistry personaRegistry;
    private final OrchestratorService orchestratorService;
    private final BotForgeMetrics metrics;
    private final ResponsePipeline responsePipeline;

    public ChatActions(
            BotForgeProperties properties,
            ChatSessionManager chatSessionManager,
            ConversationService conversationService,
            PersonaRegistry personaRegistry,
            OrchestratorService orchestratorService,
            BotForgeMetrics metrics,
            ResponsePipeline responsePipeline) {
        this.properties = properties;
        this.chatSessionManager = chatSessionManager;
        this.conversationService = conversationService;
        this.personaRegistry = personaRegistry;
        this.orchestratorService = orchestratorService;
        this.metrics = metrics;
        this.responsePipeline = responsePipeline;

        logger.info("ChatActions initialized.");
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

        var chatTimerSample = metrics.startChatTimer();
        metrics.recordChatMessage();

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

        // --- Response Pipeline ---
        responsePipeline.execute(
                effectiveProperties,
                user,
                context,
                conversation,
                conversationId,
                userPrompt,
                recentContextStr,
                workingMemory,
                chatTimerSample);
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
