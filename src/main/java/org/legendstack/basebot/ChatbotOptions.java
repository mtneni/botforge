package org.legendstack.basebot;

import com.embabel.common.ai.model.LlmOptions;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for the chatbot's conversational behaviour.
 *
 * @param llm              LLM model and hyperparameters for chat responses
 * @param messagesToEmbed  how many recent messages to include in the context
 *                         for RAG retrieval and proposition extraction
 * @param objective        the goal of the chatbot's responses
 * @param behaviour        the behaviour profile to use
 * @param persona          the persona and output style of the chatbot
 * @param maxWords         maximum number of words in a chatbot response (soft
 *                         limit)
 * @param memoryEagerLimit how many memories to eagerly load into the system
 *                         prompt via vector similarity
 * @param warmth           emotional warmth of the bot's tone (0 = clinical, 5 =
 *                         balanced, 10 = very warm)
 * @param memoryVerbosity  how explicitly the bot references remembered facts (0
 *                         = silent, 5 = natural, 10 = very explicit)
 * @param showPrompts      whether to log chat prompts sent to the LLM
 * @param showResponses    whether to log chat responses from the LLM
 * @param tagline          tagline displayed below the chatbot name in the UI
 */
public record ChatbotOptions(
                @NestedConfigurationProperty LlmOptions llm,
                @DefaultValue("20") int messagesToEmbed,
                String objective,
                String behaviour,
                String persona,
                int maxWords,
                @DefaultValue("50") int memoryEagerLimit,
                @DefaultValue("5") int warmth,
                @DefaultValue("5") int memoryVerbosity,
                @DefaultValue("true") boolean showPrompts,
                @DefaultValue("true") boolean showResponses,
                @DefaultValue("Chatbot with RAG and memory") String tagline) {
}
