package org.legendstack.basebot;

import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.reference.LlmReference;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.rag.service.SearchOperations;
import com.embabel.dice.agent.Memory;
import com.embabel.dice.projection.memory.MemoryProjector;
import com.embabel.dice.proposition.PropositionRepository;
import org.legendstack.basebot.user.BotForgeUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates the orchestrator logic that was previously inlined in
 * {@link ChatActions#respond}.
 * Breaks a complex user request into sequential sub-tasks executed by
 * specialized personas.
 */
@Service
public class OrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);

    private final PersonaRegistry personaRegistry;
    private final SearchOperations searchOperations;
    private final MemoryProjector memoryProjector;
    private final PropositionRepository propositionRepository;
    private final List<LlmReference> globalReferences;
    private final List<Tool> globalTools;

    public OrchestratorService(
            PersonaRegistry personaRegistry,
            SearchOperations searchOperations,
            MemoryProjector memoryProjector,
            PropositionRepository propositionRepository,
            List<LlmReference> globalReferences,
            List<Tool> globalTools) {
        this.personaRegistry = personaRegistry;
        this.searchOperations = searchOperations;
        this.memoryProjector = memoryProjector;
        this.propositionRepository = propositionRepository;
        this.globalReferences = globalReferences;
        this.globalTools = globalTools;
    }

    /**
     * Result of orchestration: accumulated working memory and the persona ID
     * that should handle the final synthesis step.
     */
    public record OrchestratorResult(String workingMemory, String finalPersonaId) {
    }

    /**
     * Execute the orchestrator flow: plan sub-tasks, run intermediate steps,
     * return accumulated context for the final response.
     */
    public OrchestratorResult execute(
            BotForgeProperties effectiveProperties,
            BotForgeUser user,
            String recentContextStr,
            String userPrompt,
            ActionContext context) {

        StringBuilder workingMemory = new StringBuilder();
        String finalPersonaId = "assistant";

        try {
            List<PersonaRegistry.PersonaPreset> allPersonas = personaRegistry.allForUser(user.getId());

            String personaList = allPersonas.stream()
                    .filter(p -> !"orchestrator".equals(p.id()))
                    .map(p -> "- '" + p.id() + "': " + p.description())
                    .collect(Collectors.joining("\n"));

            String prompt = """
                    Analyze the user's complex request and develop a sequential execution plan.
                    If the task is simple, return a plan with exactly ONE step assigned to the best persona.
                    If the task is complex, break it into multiple steps assigned to specialized personas.
                    Available personas:
                    %s

                    Recent Context:
                    %s

                    User Message:
                    %s
                    """.formatted(personaList, recentContextStr, userPrompt);

            OrchestrationPlan plan = context.ai()
                    .withLlm(effectiveProperties.chat().llm())
                    .createObject(prompt, OrchestrationPlan.class);

            if (plan != null && plan.steps() != null && !plan.steps().isEmpty()) {
                List<SubTask> steps = plan.steps();
                logger.info("Orchestrator generated a {}-step plan.", steps.size());

                if (steps.size() > 1) {
                    executeIntermediateSteps(steps, effectiveProperties, user, userPrompt, workingMemory, context);
                }

                SubTask finalStep = steps.get(steps.size() - 1);
                finalPersonaId = normalizePersonaId(finalStep.personaId());
                logger.info("Orchestrator selected final synthesis persona: {}", finalPersonaId);
            }
        } catch (Exception e) {
            logger.error("Orchestrator failed to plan, falling back to assistant", e);
        }

        return new OrchestratorResult(workingMemory.toString(), finalPersonaId);
    }

    private void executeIntermediateSteps(
            List<SubTask> steps,
            BotForgeProperties baseProps,
            BotForgeUser user,
            String userPrompt,
            StringBuilder workingMemory,
            ActionContext context) {

        for (int i = 0; i < steps.size() - 1; i++) {
            SubTask step = steps.get(i);
            String stepPersona = normalizePersonaId(step.personaId());

            logger.info("Executing SubTask {} of {}: [{}] -> {}", i + 1, steps.size(), stepPersona, step.objective());

            var stepProps = resolvePersonaProperties(baseProps, stepPersona);
            String stepObjective = "Sub-task Objective: " + step.objective()
                    + "\n\nOriginal User Request: " + userPrompt;
            if (workingMemory.length() > 0) {
                stepObjective += "\n\nContext from previous steps:\n" + workingMemory.toString();
            }

            var subTools = new LinkedList<>(globalTools);
            var subReferences = new LinkedList<>(globalReferences);
            subReferences.add(user.personalDocs(searchOperations));
            if (baseProps.memory().enabled()) {
                var memory = Memory.forContext(user.currentContext())
                        .withRepository(propositionRepository)
                        .withProjector(memoryProjector)
                        .withEagerSearchAbout(stepObjective, baseProps.chat().memoryEagerLimit());
                subReferences.add(memory);
                subTools.add(memory);
            }

            String stepPrompt = """
                    You are a specialized agent executing a sub-task.

                    %s

                    Formulate a complete, detailed response to satisfy this objective. Ensure your output provides everything the final synthesizer needs.
                    """
                    .formatted(stepObjective);

            SubTaskResult subMsg = context.ai()
                    .withLlm(stepProps.chat().llm())
                    .withTools(subTools)
                    .withReferences(subReferences)
                    .createObject(stepPrompt, SubTaskResult.class);

            workingMemory.append("=== Output from ").append(stepPersona).append(" ===\n");
            workingMemory.append(subMsg.output()).append("\n\n");
        }
    }

    private BotForgeProperties resolvePersonaProperties(BotForgeProperties baseProps, String personaId) {
        var resolved = personaRegistry.resolve(personaId);
        if (resolved.isPresent()) {
            var p = resolved.get();
            var newChat = new ChatbotOptions(
                    baseProps.chat().llm(), baseProps.chat().messagesToEmbed(), p.objective(), p.behaviour(), personaId,
                    baseProps.chat().maxWords(), baseProps.chat().memoryEagerLimit(), baseProps.chat().warmth(),
                    baseProps.chat().memoryVerbosity(),
                    baseProps.chat().showPrompts(), baseProps.chat().showResponses(), baseProps.chat().tagline());
            return baseProps.withChat(newChat);
        }
        return baseProps;
    }

    private String normalizePersonaId(String personaId) {
        if (personaId == null || personaId.equalsIgnoreCase("orchestrator")) {
            return "assistant";
        }
        return personaId;
    }

    // --- Structured output records for LLM calls ---

    @com.fasterxml.jackson.annotation.JsonClassDescription("An execution plan broken down into specialized sub-tasks to be performed sequentially by different personas.")
    public record OrchestrationPlan(
            @com.fasterxml.jackson.annotation.JsonPropertyDescription("The sequence of sub-tasks to execute. If the user request is simple, just return a single sub-task.") List<SubTask> steps) {
    }

    @com.fasterxml.jackson.annotation.JsonClassDescription("A single sub-task in the execution plan")
    public record SubTask(
            @com.fasterxml.jackson.annotation.JsonPropertyDescription("The ID of the persona best suited for this sub-task. Use 'assistant' if unsure.") String personaId,
            @com.fasterxml.jackson.annotation.JsonPropertyDescription("The specific objective of this sub-task.") String objective) {
    }

    @com.fasterxml.jackson.annotation.JsonClassDescription("The detailed output of the sub-task execution")
    public record SubTaskResult(
            @com.fasterxml.jackson.annotation.JsonPropertyDescription("Your full, detailed response satisfying the sub-task objective") String output) {
    }
}
