package org.legendstack.bot.astrid;

import com.embabel.agent.api.reference.LlmReference;
import com.embabel.agent.api.tool.Subagent;
import com.embabel.agent.filter.PropertyFilter;
import com.embabel.agent.rag.service.SearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.dice.common.KnowledgeType;
import com.embabel.dice.common.Relations;
import org.legendstack.basebot.rag.DocumentService;
import org.legendstack.basebot.user.BotForgeUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AstridConfiguration {

        @Bean
        Relations astridRelations() {
                return Relations.empty()
                                .withPredicatesForSubject(
                                                BotForgeUser.class, KnowledgeType.SEMANTIC,
                                                "lives in", "visited", "from", "studied at",
                                                "enjoys", "loves", "reads", "listens to", "plays")
                                .withSemanticBetween("BotForgeUser", "Pet", "owns", "user owns a pet")
                                .withSemanticBetween("BotForgeUser", "Company", "works_at", "user works at a company")
                                .withSemanticBetween("BotForgeUser", "Place", "lives_in", "user lives in a place")
                                .withSemanticBetween("BotForgeUser", "Place", "comes_from", "user comes from a place")
                                .withSemanticBetween("BotForgeUser", "Place", "has_visited", "user has visited a place")
                                .withSemanticBetween("BotForgeUser", "Place", "wants_to_visit",
                                                "user wants to visit a place")
                                .withSemanticBetween("BotForgeUser", "Place", "from", "user is from a place")
                                .withSemanticBetween("BotForgeUser", "Organization", "works_at",
                                                "user works at an organization")
                                .withSemanticBetween("BotForgeUser", "Organization", "studied_at",
                                                "user studied at an organization")
                                .withSemanticBetween("BotForgeUser", "Person", "knows", "user knows a person")
                                .withSemanticBetween("BotForgeUser", "Food", "likes", "user likes a food")
                                .withSemanticBetween("BotForgeUser", "Hobby", "enjoys", "user enjoys a hobby")
                                .withSemanticBetween("BotForgeUser", "Band", "listens_to", "user listens to a band")
                                .withSemanticBetween("BotForgeUser", "Book", "reads", "user reads a book")
                                .withSemanticBetween("BotForgeUser", "Goal", "is_working_toward",
                                                "user is working toward a goal");
        }

        @Bean
        LlmReference astrologyDocuments(SearchOperations searchOperations) {
                return new ToolishRag(
                                "astrology_docs",
                                "Shared astrology documents for answering questions about astrology, horoscopes, and related topics. Use this to answer user questions about astrology, but not for general knowledge or personal information about the user.",
                                searchOperations)
                                .withMetadataFilter(
                                                new PropertyFilter.Eq(
                                                                DocumentService.Context.CONTEXT_KEY,
                                                                DocumentService.Context.GLOBAL_CONTEXT))
                                .withUnfolding();
        }

        // As a Tool, this will be automatically picked up
        @Bean
        Subagent dailyHoroscope() {
                return Subagent.ofClass(DailyHoroscopeAgent.class)
                                .consuming(DailyHoroscopeAgent.HoroscopeRequest.class);
        }
}
